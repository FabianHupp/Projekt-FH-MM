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
                List<Connection> avoid = tryReserveRoute(route);
                if(avoid.isEmpty()){
                    //wenn man die route reservieren konnte dann darf man fahren
                    drive(route);
                    //hier meldung machen dass man finished ist
                    FdL.isFinished();
                    rec.finish(schedule);
                    return;
                }else{
                    //weiter im Algorithmus
                }
            }

            /*
            // ab hier deins martine
            //hatte vorher vergessen zu pullen und hab noch ned gemerged^^

            if (route.isEmpty()) {
                //aktuelle Position gleich Destination, Zug angekommen, ArriveEvent
            }
            if (route == null){
                //keine Route zwischen akt. Position und Destination möglich(momentan)
                //sleep??
            }
            /*
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

            //bis hier alles was du gepusht hattest*/
        }

    }

    /**
     * Versucht die gegebene Route in der allgemeinen totalen Ordnung zu reservieren.
     * Falls beim Reservieren ein Streckenteil nach der totalen Ordnung schon reserviert ist, dann gib die bereits reservierten
     * Teile wieder frei.
     * @param route
     * @return List<Connection> - leer wenn ganze route reserviert wurde, gefüllt wenn nicht der fall und mit connections
     * die avoided werden sollen
     */
    private List<Connection> tryReserveRoute(List<Connection> route){
        //do magic stuff to try to reserve a route
        List<Connection> avoid = new ArrayList<>();
        return avoid;
    }


    
    /**
     * Wenn die Route reserviert wurde, dann werden hier die gleise und bahnhöfe nacheinander freigegeben und die gleise "gefahren".
     * @param route
     */
    private void drive(List<Connection> route){
        List<Connection> copy_route = route;
        while(!copy_route.isEmpty()){
            int next_gleis = -1;            //id in liste
            int unique_gleis_id = -1;       //unique id overall

            //finde das erste Gleis dass gefahren wird
            for(Connection c: copy_route){
                if(c.first() == act_position || c.second() == act_position){
                    next_gleis = copy_route.indexOf(c);
                    unique_gleis_id = c.id();
                }
            }


            //find out if first or second are the act_position
            Location destination;
            if(copy_route.get(next_gleis).first().equals(act_position)){
                destination = copy_route.get(next_gleis).second();
            }else{
                destination = copy_route.get(next_gleis).first();
            }

            //unlocke den ersten Platz und fahre los
            FdL.FreePlace(act_position.id(),id);
            //recorder melden dass man jetzt leaved
            rec.leave(schedule,act_position);

            //travel anmelden und losfahren
            rec.travel(schedule,copy_route.get(next_gleis));
            try {
                copy_route.get(next_gleis).travel();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //Gleis freigeben und act_position ändern
            rec.arrive(schedule,destination);
            FdL.UnlockGleis(unique_gleis_id,id);
            act_position = destination;

            //gefahrenes Gleis aus der Liste entfernen
            copy_route.remove(next_gleis);
        }
    }


}
