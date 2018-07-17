package lockingTrains.impl;

import lockingTrains.shared.Map;
import lockingTrains.shared.TrainSchedule;

public class Zug implements Runnable{

    private TrainSchedule schedule;
    private int id;
    private Map map;
    FahrtdienstLeitung FdL;

    public Zug (TrainSchedule sched, int i, Map m, FahrtdienstLeitung fahrtdl){

    }


    @Override
    public void run() {

    }
}
