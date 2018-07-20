package lockingTrains.impl;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Location;
import lockingTrains.shared.Map;
import lockingTrains.shared.TrainSchedule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class Zug implements Runnable{

    private TrainSchedule schedule;
    private int id;
    private Map map;
    FahrtdienstLeitung FdL;
    Location destination;
    Location act_position;

    public Zug (TrainSchedule sched, int i, Map m, FahrtdienstLeitung fahrtdl){

        this.schedule = sched;
        this.id = i;
        this.map = m;
        this.FdL = fahrtdl;
        this.destination = sched.destination();
        this.act_position = sched.origin();

    }


    @Override
    public void run() {
        List<Connection> empty_avoid_list = new ArrayList<>();
        while(act_position != destination){
            List<Connection> route = map.route(act_position, destination, empty_avoid_list);
        }
    }
}
