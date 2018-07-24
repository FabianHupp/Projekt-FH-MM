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
        while(act_position != destination){
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
