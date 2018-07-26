package lockingTrains.impl;

import lockingTrains.shared.Connection;
import lockingTrains.shared.Map;
import lockingTrains.validation.Recorder;
import java.util.List;


public class FahrtdienstLeitung {

    private List<OwnMonitor> locations;
    private List<Connection> connections;
    private int num_trains, arrived_trains;
    private List<GleisMonitor> gleise;
    private  Recorder rec;
    private Map map;

    FahrtdienstLeitung(List<OwnMonitor> loc, Map m, List<GleisMonitor> gle, int num_trains, Recorder recorder){
        this.locations = loc;
        this.gleise = gle;
        this.connections = m.connections();
        this.num_trains = num_trains;
        arrived_trains = 0;
        rec = recorder;
        this.map = m;
    }


    synchronized boolean lockGleis(int gleisid,int train_id){
        int correct_gleis = -1;
        for(GleisMonitor gm : gleise){
            if(gm.getId() == gleisid){
                correct_gleis = gleise.indexOf(gm);
            }
        }
        boolean reserved = false;
        if(correct_gleis != -1){
            reserved = gleise.get(correct_gleis).reserve(train_id);
        }
        return reserved;
    }

    synchronized boolean ReservePlace(int stopid, int train_id){
        int correct_monitor = -1;
        for(OwnMonitor om : locations){
            if(om.getId() == stopid){
                correct_monitor = locations.indexOf(om);
            }
        }
        boolean reserved = false;
        if(correct_monitor != -1) {
           reserved = locations.get(correct_monitor).reserve(train_id);
        }
        return reserved;
    }

    synchronized void UnlockGleis(int gleisid, int train_id){
        int correct_gleis = -1;
        for(GleisMonitor gm : gleise){
            if(gm.getId() == gleisid){
                correct_gleis = gleise.indexOf(gm);
            }
        }
        if(correct_gleis != -1){
            gleise.get(correct_gleis).free_track(train_id);
        }
        notifyAll();
    }

    synchronized void FreePlace(int stopid, int train_id){
        int correct_monitor = -1;
        for(OwnMonitor om : locations){
            if(om.getId() == stopid){
                correct_monitor = locations.indexOf(om);
            }
        }
        if(correct_monitor != -1) {
            locations.get(correct_monitor).free_space(train_id);
        }
        notifyAll();
    }

    synchronized boolean checkDone(){
        return arrived_trains == num_trains;
    }

    synchronized void isFinished(){
            arrived_trains++;
    }

    synchronized void waitforFdL(){
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
