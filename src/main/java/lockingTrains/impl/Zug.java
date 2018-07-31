package lockingTrains.impl;

import lockingTrains.shared.*;
import lockingTrains.validation.Recorder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Zug implements Runnable {

    private TrainSchedule schedule;
    private int id;
    private Map map;
    private FahrtdienstLeitung FdL;
    private Location destination;
    private Location act_position;
    private Recorder rec;
    private Condition wait;
    private Lock lock;
    private boolean paused = false;

    Zug(TrainSchedule sched, int i, Map m, FahrtdienstLeitung fahrtdl, Recorder recorder) {
        this.schedule = sched;
        this.id = i;
        this.map = m;
        this.FdL = fahrtdl;
        this.destination = sched.destination();
        this.act_position = sched.origin();
        rec = recorder;
        wait = fahrtdl.getGleis_frei();
        lock = fahrtdl.getWaitforfdl_lock();
    }

    @Override
    public void run() {
        //trainschedule startet:
        rec.start(schedule);
        System.out.println("Startet train with id: " + id);
        List<Position> empty_avoid_list = new ArrayList<>();

        while (!act_position.equals(destination)) {
            List<Connection> route = map.route(act_position, destination, empty_avoid_list);

            //falls keine Route existiert obwohl leere avoid-Liste => fail
            if (route == null) {
                return;
            }

            System.out.println("Routesize for " + id + " is " + route.size());

            //wenn die route leer ist, dann finished, kein arrive event weil man ja schon vorher angekommen ist;
            //return beendet thread
            if (route.isEmpty()) {
                FdL.isFinished();
                rec.finish(schedule);
                return;
            }
            //wenn die route nicht null und nicht leer ist, gibt es eine reservierbare route
            List<Position> avoid = tryReserveRoute(route);
            if (avoid.isEmpty()) {
                //wenn man die route reservieren konnte dann darf man fahren
                drive(route, destination);
                //hier meldung machen dass man finished ist
                System.out.println("Zug " + id + " ist gefahren und angekommen.");
                FdL.isFinished();
                rec.finish(schedule);
                return;
            }

            //reservieren hat nicht geklappt => nochmal veruschen mit gleisen in der avoid-liste
            route = map.route(act_position, destination, avoid);
            //solange wie mit neuen avoid eine neue route existiert
            while (route != null) {
                //falls sie nicht leer ist
                List<Position> new_avoid = tryReserveRoute(route);
                if (new_avoid.isEmpty()) {
                    drive(route, destination);
                    FdL.isFinished();
                    rec.finish(schedule);
                    return;
                }
                //falls neue avoids vorhanden zu avoid adden
                avoid.addAll(new_avoid);

                //neue route berechnen für nächste while-schleifen wiederholung
                route = map.route(act_position, destination, avoid);
            }

            //wenn keine route mit avoids gefunden wurde
            //gehe zu Schritt 3: nächsten Bahnhof finden und warten
            avoid.clear();
            route = map.route(act_position, destination, empty_avoid_list);
            //findet den nächsten stop und reserviert einen platz
            Location nex_stop = find_next_stop(route);

            //jetzt nochmal route bis zum reservierten parkplatz bestimmen
            route = map.route(act_position, nex_stop, empty_avoid_list);

            //streckenteile bei FdL reservieren und warten
            boolean reserved = false;
            System.out.println("NextStop" + id + " ist " + nex_stop);
            while(!reserved ) {
                List<Position> unnec = tryReserveRoute(route);
                if (unnec.isEmpty()) {
                    reserved = true;
                    drive(route, nex_stop);
                    if (act_position == destination) {
                        FdL.isFinished();
                        rec.finish(schedule);
                        return;
                    }
                    /*if (!act_position.isStation()) {
                        rec.pause(schedule, act_position);
                    }*/
                } else {
                    lock.lock();
                    try {
                        wait.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }

                }
            }


            //return to start of algorithmn
        }

    }

    /**
     * Versucht die gegebene Route in der allgemeinen totalen Ordnung zu reservieren.
     * Falls beim Reservieren ein Streckenteil nach der totalen Ordnung schon reserviert ist, dann gib die bereits reservierten
     * Teile wieder frei.
     *
     * @param route route to be reserved
     * @return List<Connection> - leer wenn ganze route reserviert wurde, gefüllt wenn nicht der fall und mit connections
     * die avoided werden sollen
     */
    private List<Position> tryReserveRoute(List<Connection> route) {
        List<Connection> copy_route = copy(route);
        List<Connection> save_reserved = new ArrayList<>();
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

            //Gleis reservieren, falls es failt dann Connection in avoid einfügen
            boolean reserved = this.FdL.lockGleis(smallestId, this.id);
            if (reserved) {
                save_reserved.add(copy_route.get(list_id));
            } else {
                avoid.add(copy_route.get(list_id).first());
                avoid.add(copy_route.get(list_id).second());
                reverse_reservation(save_reserved);
                return avoid;
            }
            //locations reservieren
            boolean reserved_place_first = this.FdL.reserve_Einfahrt(copy_route.get(list_id).first().id(), this.id);
            if (!reserved_place_first) {
                avoid.add(copy_route.get(list_id).first());
                reverse_reservation(save_reserved);
                return avoid;
            }
            boolean reserved_place_second = this.FdL.reserve_Einfahrt(copy_route.get(list_id).second().id(), this.id);
            if (!reserved_place_second) {
                avoid.add(copy_route.get(list_id).second());
                reverse_reservation(save_reserved);
                return avoid;
            }

            //wenn allerdings nichts gefailed ist, dann das element aus copy_route entfernen, und weiter mit nächstem gleis
            copy_route.remove(list_id);
        }

        return avoid;
    }

    /**
     * Macht die Reservierung eines Zuges bis zu einer bestimmten Stelle Rückgängig wenn nicht die ganze Strecke reservierbar ist
     *
     * @param save_reserved connections to be reversed
     */
    private void reverse_reservation(List<Connection> save_reserved) {
        for (Connection c : save_reserved) {
            FdL.UnlockGleis(c.id(), this.id);
            FdL.free_Einfahrt(c.first().id(), this.id);
            FdL.free_Einfahrt(c.second().id(), this.id);
        }
    }


    /**
     * Wenn die Route reserviert wurde, dann wird hier "gefahren" und Gleise und alle Zwischenstops freigegeben.
     *
     * @param route route to be driven
     */
    private void drive(List<Connection> route, Location goal) {
        List<Connection> copy_route = copy(route);
        if(paused){
            if (!act_position.isStation()) {
                rec.resume(schedule, act_position);
            }
        }
        while (!copy_route.isEmpty()) {
            int next_gleis = -1;            //id in liste
            int unique_gleis_id = -1;       //unique id overall

            //finde das erste Gleis dass gefahren wird
            for (Connection c : copy_route) {
                if (c.first() == act_position || c.second() == act_position) {
                    next_gleis = copy_route.indexOf(c);
                    unique_gleis_id = c.id();
                }
            }

            //find out if first or second are the act_position
            Location desti;
            if (copy_route.get(next_gleis).first().equals(act_position)) {
                desti = copy_route.get(next_gleis).second();
            } else {
                desti = copy_route.get(next_gleis).first();
            }

            System.out.println(id + " wählt und fährt Gleis " + unique_gleis_id + " von " + act_position + " nach " + desti);

            //unlocke den ersten ParkPlatz (man hat immernoch die Einfahrt reserviert)
            FdL.FreePlace(act_position.id(), id);

            //recorder melden dass man jetzt leaved und Einfahrt frei machen
            rec.leave(schedule, act_position);

            //travel anmelden und losfahren
            rec.travel(schedule, copy_route.get(next_gleis));
            FdL.free_Einfahrt(act_position.id(), id);
            try {
                copy_route.get(next_gleis).travel();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //Gleis freigeben und act_position ändern
            rec.arrive(schedule, desti);
            if(desti == goal && !desti.isStation()){
                rec.pause(schedule,desti);
                paused = true;
            }
            FdL.UnlockGleis(unique_gleis_id, id);
            act_position = desti;

            //gefahrenes Gleis aus der Liste entfernen
            copy_route.remove(next_gleis);
        }

        if (act_position == goal) {
            FdL.free_Einfahrt(goal.id(), id);
        }
    }

    /**
     * Copy-funktin für eine Liste
     *
     * @param route original-route zum Kopieren
     * @return Neue listem it kopiertem inhalt
     */
    private List<Connection> copy(List<Connection> route) {
        return new ArrayList<>(route);
    }

    /**
     * Findet die nächste Parkmöglichkeit auf dem Weg
     *
     * @param route
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

            boolean reserved = false;
            if (next_loc.isStation()) {
                if (next_loc.equals(destination)) {
                    reserved = FdL.ReservePlace(next_loc.id(), id);
                    if (reserved) {
                        return next_loc;
                    }
                }
            }else {
                if (next_loc.capacity() > 0) {
                    reserved = FdL.ReservePlace(next_loc.id(), id);
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
