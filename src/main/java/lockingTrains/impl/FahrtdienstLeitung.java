package lockingTrains.impl;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Map;

import java.util.List;

public class FahrtdienstLeitung {

    List<OwnMonitor> locations;
    List<Connection> connections;
    Map map;
    List<GleisMonitor> gleise;
    boolean done;

    public FahrtdienstLeitung(List<OwnMonitor> loc, Map m, List<GleisMonitor> gle){
        locations = loc;
        map = m;
        gleise = gle;
        connections =m.connections();
        done = false;
    }

    synchronized boolean checkDone(){
        return done;
    }
}
