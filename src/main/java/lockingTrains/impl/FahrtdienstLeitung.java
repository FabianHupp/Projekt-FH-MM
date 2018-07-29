package lockingTrains.impl;

import lockingTrains.shared.Map;
import lockingTrains.validation.Recorder;
import java.util.List;


public class FahrtdienstLeitung {

    private List<OwnMonitor> locations;
    private int num_trains, arrived_trains;
    private List<GleisMonitor> gleise;
    private  Recorder rec;
    private Map map;

    FahrtdienstLeitung(List<OwnMonitor> loc, Map m, List<GleisMonitor> gle, int num_trains, Recorder recorder){
        this.locations = loc;
        this.gleise = gle;
        this.num_trains = num_trains;
        arrived_trains = 0;
        rec = recorder;
        this.map = m;
    }

    /**
     * Lockt ein Gleis
     * @param gleisid       Gleisid
     * @param train_id      Zugid
     * @return true wenn reseviert wurde, false wenn schon belegt
     */
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

    /**
     * reserviert einen Platz in einem Bahnhof
     * @param stopid            Locationid
     * @param train_id          Zugid
     * @return true wenn es geklappt hat, false wenn nicht
     */
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

    /**
     * Gibt ein Gleis frei
     * @param gleisid
     * @param train_id
     */
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

    /**
     * Gibt einen Platz frei
     * @param stopid    id der Location
     * @param train_id  id des Zuges
     */
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


    /**
     * gibt an ob alle Trains regelgemäß terminiert sind
     * @return ob alle trains terminiert sind
     */
    synchronized boolean checkDone(){
        return arrived_trains == num_trains;
    }

    /**
     * kann von einem Zug aufgerufen werden um zu signalisieren dass er fertig ist
     */
    synchronized void isFinished(){
            arrived_trains++;
    }

    /**
     * kann von einem Zug aufgerufen werden um auf die Freigabe eines Locks zu warten
     */
    synchronized void waitforFdL(){
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
