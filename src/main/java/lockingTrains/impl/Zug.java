package lockingTrains.impl;

import lockingTrains.shared.*;
import lockingTrains.validation.Recorder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

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
            route = map.route(act_position, destination, empty_avoid_list);
            //Findet den nächsten stop und reserviert einen Platz
            Location nex_stop = find_next_stop(route);
            //Route bis zum reservierten parkplatz bestimmen
            route = map.route(act_position, nex_stop, empty_avoid_list);


            //Streckenteile bei FdL reservieren und blockierend warten wenn nicht frei
            List<Connection> copy_route = copy(route);
            while(!copy_route.isEmpty()){
                int list_id = 0;
                int smallestId = copy_route.get(0).id();
                for (Connection c : copy_route) {
                    if (c.id() < smallestId) {
                        smallestId = c.id();
                        list_id = copy_route.indexOf(c);
                    }
                }

                FdL.lockGleisBlocking(smallestId,this.id);
                FdL.reserveEinfahrtBlocking(copy_route.get(list_id).first().id(), this.id);
                FdL.reserveEinfahrtBlocking(copy_route.get(list_id).second().id(), this.id);
                copy_route.remove(list_id);
            }
            //Route reserviert
            drive(route,nex_stop);
            if(nex_stop == destination){
                FdL.isFinished();
                rec.finish(schedule);
                return;
            }
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
        List<Connection> save_reserved = new ArrayList<>();
        List<Connection> copy_route = copy(route);
        List<Position> avoid = new ArrayList<>();

        while (!copy_route.isEmpty()) {
            int list_id = 0;                            //eindeutige position in der Liste
            int smallestId = copy_route.get(0).id();    //niedrigste id der Gleise
            for (Connection c : copy_route) {
                if (c.id() < smallestId) {
                    smallestId = c.id();
                    list_id = copy_route.indexOf(c);
                }
            }

            //Gleis reservieren
            boolean reserved = FdL.lockGleis(smallestId, this.id);
            if (reserved) {
                save_reserved.add(copy_route.get(list_id));
            } else {
                avoid.add(copy_route.get(list_id).first());
                avoid.add(copy_route.get(list_id).second());
                reverse_reservation(save_reserved);
                return avoid;
            }
            //Locations reservieren
            boolean reserved_place_first = FdL.reserve_Einfahrt(copy_route.get(list_id).first().id(), this.id);
            if (!reserved_place_first) {
                avoid.add(copy_route.get(list_id).first());
                reverse_reservation(save_reserved);
                return avoid;
            }
            boolean reserved_place_second = FdL.reserve_Einfahrt(copy_route.get(list_id).second().id(), this.id);
            if (!reserved_place_second) {
                avoid.add(copy_route.get(list_id).second());
                reverse_reservation(save_reserved);
                return avoid;
            }
            copy_route.remove(list_id);
        }

        return avoid;
    }




    /**
     * Macht die Reservierung eines Zuges bis zu einer bestimmten Stelle Rückgängig wenn nicht die ganze Strecke reservierbar ist.
     * @param save_reserved Connections die schon reserviert wurden.
     */
    private void reverse_reservation(List<Connection> save_reserved) {
        for (Connection c : save_reserved) {
            FdL.UnlockGleis(c.id(), this.id);
            FdL.free_Einfahrt(c.first().id(), this.id);
            FdL.free_Einfahrt(c.second().id(), this.id);
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
                if (next_loc.equals(destination)) {
                    boolean reserved = FdL.ReservePlace(next_loc.id(), id);
                    if (reserved) {
                        return next_loc;
                    }
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

}
