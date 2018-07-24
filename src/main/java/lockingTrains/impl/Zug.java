package lockingTrains.impl;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.Map;
import lockingTrains.shared.TrainSchedule;
import lockingTrains.validation.Recorder;

import java.util.ArrayList;
import java.util.List;


public class Zug implements Runnable{

    private TrainSchedule schedule;
    private int id;
    private Map map;
    FahrtdienstLeitung FdL;
    Location destination;
    Location act_position;
    Recorder rec;

    public Zug (TrainSchedule sched, int i, Map m, FahrtdienstLeitung fahrtdl, Recorder recorder){
        this.schedule = sched;
        this.id = i;
        this.map = m;
        this.FdL = fahrtdl;
        this.destination = sched.destination();
        this.act_position = sched.origin();
        rec = recorder;
    }


    @Override
    public void run() {

        //trainschedule startet:
        rec.start(schedule);

        List<Connection> empty_avoid_list = new ArrayList<>();
        while(!act_position.equals(destination)){
            List<Connection> route = map.route(act_position, destination, empty_avoid_list);
            //wenn eine route gefunden wurde
            if(route != null){
                //wenn die route leer ist, dann finished, kein arrive event weil man ja schon vorher angekommen ist;
                // return beendet thread
                if(route.isEmpty()){
                    FdL.isFinished();
                    rec.finish(schedule);
                    return;
                }
                //wenn die route nicht null und nicht leer ist, gibt es eine reservierbare route
                boolean res = tryReserveRoute(route);
                if(res){
                    //wenn man die route reservieren konnte dann darf man fahren
                    drive(route);
                    //hier meldung machen dass man arrived und finished ist
                    FdL.isFinished();
                    rec.arrive(schedule,destination);
                    rec.finish(schedule);
                    return;
                }else{
                    //weiter im Algorithmus
                }
            }


            // ab hier deins martine
            //hatte vorher vergessen zu pullen und hab noch ned gemerged^^
            if (route.isEmpty()) {
                //aktuelle Position gleich Destination, Zug angekommen, ArriveEvent
            }
            if (route == null){
                //keine Route zwischen akt. Position und Destination möglich(momentan)
                //sleep??
            }
            //Liste ist nicht leer oder null, es existiert eine mögl. Route
            //gibt es zu vermeidende Streckenteile?(avoid)
            //wenn nein, dann gehe weiter zum Reservieren
            //wenn ja, füge diese an avoid an, zurück zum Schleifenkopf, neue Route berechnen
            List<Connection> toAvoid = this.FdL.avoid(route);
            if (!toAvoid.isEmpty()) {
                empty_avoid_list.addAll(toAvoid);
                continue;
            }

            //Reservieren der Connections aus der Liste nach totaler Ordnung(aufsteigend)
            //finde das Gleis mit der kleinsten Id
            while(!route.isEmpty()) {
                int i = 1;
                int smallestId = route.get(0).id();
                int index = 0;
                while (i < route.size()) {
                    if (route.get(index).id() < smallestId) {
                        smallestId = route.get(index).id();
                        index = i;
                    }
                    i++;
                }
                //Gleis reservieren, falls schon reserviert geht in avoid List, falls nicht reservieren
                this.FdL.lockGleis(smallestId, this.id);

                //entferne reservierten Teil aus Liste
                route.remove(route.get(index));
            }

            //bis hier alles was du gepusht hattest
        }

    }

    /**
     * Versucht die gegebene Route in der allgemeinen totalen Ordnung zu reservieren.
     * Falls beim Reservieren ein Streckenteil nach der totalen Ordnung schon reserviert ist, dann gib die bereits reservierten
     * Teile wieder frei.
     * @param route
     * @return true wenn die route ganz reserviert wurden konnte, false wenn mittendrin zurückgenommen wurde
     */
    private boolean tryReserveRoute(List<Connection> route){
        //do magic stuff to try to reserve a route
        //false if it fails
        return true;
    }

    /**
     * Wenn die Route reserviert wurde, dann werden hier die gleise und bahnhöfe nacheinander freigegeben und die gleise "gefahren".
     * @param route
     */
    private void drive(List<Connection> route){
        //do magic stuff to drive from a to b;
    }


}
