package lockingTrains.impl;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Map;

import java.util.ArrayList;
import java.util.List;

public class FahrtdienstLeitung {

    List<OwnMonitor> locations;
    List<Connection> connections;
    Map map;
    List<GleisMonitor> gleise;
    boolean done;

    public FahrtdienstLeitung(List<OwnMonitor> loc, Map m, List<GleisMonitor> gle){
        this.locations = loc;
        this.map = m;
        this.gleise = gle;
        this.connections =m.connections();
        this.done = false;
    }

    synchronized boolean checkDone(){
        return done;
    }

    public void reserviereGleis(int gleisId, int zugId) {
        //angenommen die Gleise sind von 0 bis ... durchnummeriert und sind sortiert in der Liste
        gleise.get(gleisId).reserve(zugId);
    }

    public List<Connection> avoid(List<Connection> route){
        int i = 0;
        List<Connection> avoidList = new ArrayList<>();
        while (i < route.size()) {

            int id = route.get(i).id();

            //angenommen die Gleise sind von 0 bis ... durchnummeriert und sind sortiert in der Liste
            if (gleise.get(id).getReserved()) {
                avoidList.add(route.get(i));
            }

            i++;
        }
        return avoidList;
    }
}
