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
            boolean res = tryReserveRoute(route);
            if(res){
                drive(route);
                FdL.isArrived();
                //hier meldung machen dass man arrived und finished ist
                rec.arrive(schedule,destination);
                rec.finish(schedule);
                return;
            }else{
                //weiter im Algorithmus
            }

            // von hier deins martine^^... ich kann irgendwie nicht mergen :D

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


    private boolean tryReserveRoute(List<Connection> route){
        //do magic stuff to try to reserve a route
        //false if it fails
        return false;
    }

    private void drive(List<Connection> route){
        //do magic stuff to drive from a to b;
    }


}
