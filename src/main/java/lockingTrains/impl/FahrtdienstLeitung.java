package lockingTrains.impl;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Map;
import lockingTrains.validation.Recorder;

import java.util.ArrayList;
import java.util.List;

public class FahrtdienstLeitung {

    private List<OwnMonitor> locations;
    private List<Connection> connections;
    private int num_trains, arrived_trains;
    private List<GleisMonitor> gleise;
    private  Recorder rec;
    private Map map;

    public FahrtdienstLeitung(List<OwnMonitor> loc, Map m, List<GleisMonitor> gle, int num_trains, Recorder recorder){
        this.locations = loc;
        this.gleise = gle;
        this.connections = m.connections();
        this.num_trains = num_trains;
        arrived_trains = 0;
        rec = recorder;
        this.map = m;
    }


    public boolean lockGleis(int gleisid,int train_id){
        return gleise.get(gleisid).reserve(train_id);
    }

    public boolean ReservePlace(int stopid, int train_id){
        return locations.get(stopid).reserve();
    }

    public void UnlockGleis(int gleisid, int train_id){
        int correct_gleis = -1;
        for(GleisMonitor gm : gleise){
            if(gm.getId() == gleisid){
                correct_gleis = gleise.indexOf(gm);
            }
        }
        if(correct_gleis != -1){
            gleise.get(correct_gleis).free_track(train_id);
        }
    }

    public void FreePlace(int stopid, int train_id){
        int correct_monitor = -1;
        for(OwnMonitor om : locations){
            if(om.getId() == stopid){
                correct_monitor = locations.indexOf(om);
            }
        }
        if(correct_monitor != -1) {
            locations.get(correct_monitor).free_space();
        }
    }

    synchronized boolean checkDone(){
        return arrived_trains == num_trains;
    }

    synchronized void isFinished(){
            arrived_trains++;
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
