package lockingTrains.impl;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Map;
import lockingTrains.validation.Recorder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

public class FahrtdienstLeitung {

    List<OwnMonitor> locations;
    List<Connection> connections;
    int num_trains, arrived_trains;
    Map map;
    List<GleisMonitor> gleise;
    Recorder rec;
    Lock lock;

    public FahrtdienstLeitung(List<OwnMonitor> loc, Map m, List<GleisMonitor> gle, int num_trains, Recorder recorder, Lock lock){
        this.locations = loc;
        this.map = m;
        this.gleise = gle;
        this.connections =m.connections();
        this.num_trains = num_trains;
        arrived_trains = 0;
        rec = recorder;
        this.lock = lock;
    }


    public boolean LockGleis(int gleisid,int train_id){
        return gleise.get(gleisid).reserve(train_id);
    }

    public boolean ReservePlace(int stopid, int train_id){
        return locations.get(stopid).reserve();
    }

    public void UnlockGleis(int gleisid, int train_id){
        gleise.get(gleisid).free_track(train_id);
    }

    public void FreePlace(int stopid, int train_id){
        locations.get(stopid).free_space();
    }

    synchronized boolean checkDone(){
        if(arrived_trains == num_trains){
            return true;
        }
        return  false;
    }

    synchronized void isArrived(){
            arrived_trains++;
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
