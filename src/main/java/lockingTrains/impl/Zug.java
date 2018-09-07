package lockingTrains.impl;

import lockingTrains.shared.*;
import lockingTrains.validation.Recorder;
import java.util.ArrayList;
import java.util.List;

public class Zug implements Runnable {

    private Location destination, act_position;
    private boolean paused = false;
    private TrainSchedule schedule;
    private FahrtdienstLeitung FdL;
    private Recorder rec;
    private Map map;
    private int id;

    Zug(TrainSchedule sched, int i, Map m, FahrtdienstLeitung fahrtdl, Recorder recorder) {
        destination = sched.destination();
        act_position = sched.origin();
        schedule = sched;
        rec = recorder;
        FdL = fahrtdl;
        map = m;
        id = i;
    }

    @Override
    public void run() {
        rec.start(schedule);
        List<Position> empty_avoid_list = new ArrayList<>();

        while (!act_position.equals(destination)) {
            List<Connection> route = map.route(act_position, destination, empty_avoid_list);

            //Falls keine Route existiert obwohl leere avoid-Liste => fail
            if (route == null) {return; }
            //Wenn schon an Ziel
            if (route.isEmpty()) {
                FdL.isFinished();
                rec.finish(schedule);
                return;
            }
            //Es gibt eine reservierbare route
            List<Position> avoid = tryReserveRoute(route);
            if (avoid.isEmpty()) {
                drive(route, destination);
                System.out.println("Zug " + id + " ist gefahren und angekommen.");
                FdL.isFinished();
                rec.finish(schedule);
                return;
            }

            //Schritt 2: Reservieren hat nicht geklappt => nochmal mit avoid-liste
            route = map.route(act_position, destination, avoid);
            //Solange eine route gefunden wird:
            while (route != null) {
                List<Position> new_avoid = tryReserveRoute(route);
                if (new_avoid.isEmpty()) {
                    drive(route, destination);
                    FdL.isFinished();
                    rec.finish(schedule);
                    return;
                }
                //Neue avoids adden
                avoid.addAll(new_avoid);

                //Neue route für nächste schleifen-wiederholung
                route = map.route(act_position, destination, avoid);
            }

            //Schritt 3: nächsten Stellplatz finden und warten
            avoid.clear();

            route = map.route(act_position, destination, empty_avoid_list);
            //Findet den nächsten stop und reserviert einen Platz
            Location nex_stop = find_next_stop(route);
            //Route bis zum reservierten parkplatz bestimmen
            route = map.route(act_position, nex_stop, empty_avoid_list);

            reserveblocking(route);

            drive(route,nex_stop);
            if(act_position.equals(destination)){
                System.out.println("Zug " + id + " ist gefahren und angekommen.");
                FdL.isFinished();
                rec.finish(schedule);
                return;
            }
        }

    }

    private void reserveblocking(List<Connection> route) {
        List<GleisMonitor> rou =FdL.getRoute(route);
        while(!rou.isEmpty()){
            int list_id = 0;
            int smallest_total_id = rou.get(0).getTotalid();
            int smallestid = rou.get(0).getId();
            boolean einfahrt = rou.get(0).getIsEinfahrt();
            for(GleisMonitor gm:rou){
                int gmid = gm.getTotalid();
                if(gmid < smallest_total_id){
                    smallestid = gm.getId();
                    smallest_total_id = gmid;
                    list_id = rou.indexOf(gm);
                    einfahrt = gm.getIsEinfahrt();
                }
            }

            if(!einfahrt){
                FdL.lock_Gleis_block(smallestid,id);
            }else{
                FdL.reserve_Einfahrt_block(smallestid, id);
            }

            rou.remove(list_id);
        }
    }

    /**
     * Versucht die gegebene Route in der allgemeinen totalen Ordnung zu reservieren.
     * Falls beim Reservieren ein Streckenteil nach der totalen Ordnung schon reserviert ist, dann werden die bereits reservierten
     * Teile wieder frei gegeben.
     * @param route Route zum Reservieren
     * @return List<Connection> - leer wenn ganze route reserviert wurde, gefüllt mit connections
     *         die avoided werden sollen wenn dem nicht der fall ist
     */
    private List<Position> tryReserveRoute(List<Connection> route) {
        List<GleisMonitor> save_reserved = new ArrayList<>();
        List<GleisMonitor> rou = FdL.getRoute(route);
        List<Position> avoid = new ArrayList<>();
        boolean reserved = false;
        while(!rou.isEmpty()){
            int list_id = 0;
            int smallest_total_id = rou.get(0).getTotalid();
            int smallestid = rou.get(0).getId();
            boolean einfahrt = rou.get(0).getIsEinfahrt();
            for(GleisMonitor gm : rou){
                int gmid = gm.getTotalid();
                if(gmid < smallest_total_id){
                    smallestid = gm.getId();
                    smallest_total_id = gmid;
                    list_id = rou.indexOf(gm);
                    einfahrt = gm.getIsEinfahrt();
                }
            }

            if(!einfahrt){
                reserved = FdL.lockGleis(smallestid,id);
            }else{
                reserved = FdL.reserve_Einfahrt(smallestid,id);
            }

            if(reserved){
                save_reserved.add(rou.get(list_id));
            }else{
                reverse_reservation(save_reserved);
                if(einfahrt){
                    avoid.add(getPosEin(smallestid));
                }else{
                    avoid.addAll(getPosGleis(smallestid));
                }
                return avoid;
            }

            rou.remove(list_id);
        }
        return avoid;
    }




    /**
     * Macht die Reservierung eines Zuges bis zu einer bestimmten Stelle Rückgängig wenn nicht die ganze Strecke reservierbar ist.
     * @param save_reserved Connections die schon reserviert wurden.
     */
    private void reverse_reservation(List<GleisMonitor> save_reserved){
        for (GleisMonitor c : save_reserved) {
            if(c.getIsEinfahrt()){
                FdL.free_Einfahrt(c.getId(),id);
            }else{
                FdL.UnlockGleis(c.getId(),id);
            }
        }
    }


    /**
     * Wenn die Route reserviert wurde, fahren und Gleise sowie alle Zwischenstops freigeben.
     * @param route Route die zurückgelegt wird.
     */
    private void drive(List<Connection> route, Location goal) {
        List<Connection> copy_route = copy(route);
        //Pause beenden fals Zwischenstop eingelegt
        if(paused){
            if (!act_position.isStation()) {
                rec.resume(schedule, act_position);
            }
        }
        //Fahren:
        while (!copy_route.isEmpty()) {
            int next_gleis = -1;
            int unique_gleis_id = -1;
            for (Connection c : copy_route) {
                if (c.first() == act_position || c.second() == act_position) {
                    next_gleis = copy_route.indexOf(c);
                    unique_gleis_id = c.id();
                }
            }
            //First or Second == Act-Position?
            Location desti;
            if (copy_route.get(next_gleis).first().equals(act_position)) {
                desti = copy_route.get(next_gleis).second();
            } else {
                desti = copy_route.get(next_gleis).first();
            }
            System.out.println(id + " wählt und fährt Gleis " + unique_gleis_id + " von " + act_position + " nach " + desti);
            //Unlock ersten ParkPlatz (immernoch Einfahrt reserviert)
            FdL.FreePlace(act_position.id(), id);
            //Recorder melden dass man leaved
            rec.leave(schedule, act_position);
            //Travel anmelden, Einfahrt frei machen, fahren
            rec.travel(schedule, copy_route.get(next_gleis));
            FdL.free_Einfahrt(act_position.id(), id);
            try {
                copy_route.get(next_gleis).travel();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //Arrive anmelden, evtl Pause anmelden, und Gleis freigeben
            rec.arrive(schedule, desti);
            if(desti == goal && !desti.isStation()){
                rec.pause(schedule,desti);
                paused = true;
            }
            FdL.UnlockGleis(unique_gleis_id, id);
            act_position = desti;
            copy_route.remove(next_gleis);
        }

        if (act_position == goal) {
            System.out.println("reached_goal_drive: id: " + id);
            FdL.free_Einfahrt(goal.id(), id);
        }
    }



    /**
     * Copy-funktin für eine Liste
     * @param route original-route zum Kopieren
     * @return Neue listem it kopiertem inhalt
     */
    private List<Connection> copy(List<Connection> route) {
        return new ArrayList<>(route);
    }



    /**
     * Findet die nächste Parkmöglichkeit auf dem Weg
     * @param route route auf dem ein freier platz gefunden werden soll
     * @return nächster freier Platz
     */
    private Location find_next_stop(List<Connection> route) {
        List<Connection> copy_route = copy(route);
        Location last_loc = act_position;
        Location next_loc = act_position;
        while (!copy_route.isEmpty()) {
            int list_id = 0;
            for (Connection c : copy_route) {
                if (c.first() == last_loc || c.second() == last_loc) {
                    list_id = copy_route.indexOf(c);
                    if (c.first() == last_loc) {
                        next_loc = c.second();
                    } else {
                        next_loc = c.first();
                    }

                }
            }
            if (next_loc.isStation()) {
                    boolean reserved = FdL.ReservePlace(next_loc.id(), id);
                    if (reserved) {
                        return next_loc;
                    }
            }else {
                if (next_loc.capacity() > 0) {
                    boolean reserved = FdL.ReservePlace(next_loc.id(), id);
                    if (reserved) {
                        return next_loc;
                    }
                }
            }
            last_loc = next_loc;
            copy_route.remove(list_id);
        }
        return next_loc;
    }


    private Location getPosEin(int locid){
        Location ret = new Location("dummy", Location.Capacity.INFINITE, 0, 0);
        for(Location l : map.locations()){
            if(l.id() == locid){
                ret = l;
            }
        }
        return ret;
    }

    private List<Location> getPosGleis(int gleisid){
        List<Location> ret = new ArrayList<>();
        for(Connection c : map.connections()){
            if(c.id() == gleisid){
                ret.add(c.first());
                ret.add(c.second());
            }
        }
        return ret;
    }
}
