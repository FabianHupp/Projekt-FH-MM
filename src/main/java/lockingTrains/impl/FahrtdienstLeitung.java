package lockingTrains.impl;

import lockingTrains.shared.Map;
import lockingTrains.validation.Recorder;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class FahrtdienstLeitung {

    private List<OwnMonitor> locations;
    private int num_trains, arrived_trains;
    private List<GleisMonitor> gleise;
    private Lock waitforfdl_lock, gleise_lock, location_lock, arrived_trains_lock;
    private Condition Gleis_frei;   //CONDITION Predicate: new_gleis_frei = true
    private boolean new_gleis_frei;

    FahrtdienstLeitung(List<OwnMonitor> loc, Map m, List<GleisMonitor> gle, int num_trains, Recorder recorder){
        this.locations = loc;
        this.gleise = gle;
        this.num_trains = num_trains;
        arrived_trains = 0;
        waitforfdl_lock = new ReentrantLock();
        gleise_lock = new ReentrantLock();
        location_lock = new ReentrantLock();
        Gleis_frei = waitforfdl_lock.newCondition();
        arrived_trains_lock = new ReentrantLock();
        new_gleis_frei = false;
    }

    /**
     * Lockt ein Gleis
     * @param gleisid       Gleisid
     * @param train_id      Zugid
     * @return true wenn reseviert wurde, false wenn schon belegt
     */
    public boolean lockGleis(int gleisid,int train_id){
        //hier kein lock, da auf daten in den gleisen zugegriffen wird, die sich nie im verlauf desprogrammes verändern (ID)
        int correct_gleis = -1;
        for(GleisMonitor gm : gleise){
            if(gm.getId() == gleisid){
                correct_gleis = gleise.indexOf(gm);
            }
        }
        boolean reserved = false;
        GleisMonitor gm;
        gleise_lock.lock();
        try{
            gm = gleise.get(correct_gleis);
        }finally {
            gleise_lock.unlock();
        }
        if(correct_gleis != -1){
            reserved = gm.reserve(train_id);
        }
        return reserved;
    }

    /**
     * reserviert einen Platz in einem Bahnhof
     * @param stopid            Locationid
     * @param train_id          Zugid
     * @return true wenn es geklappt hat, false wenn nicht*/
    public boolean ReservePlace(int stopid, int train_id){
        int correct_monitor = -1;
        for(OwnMonitor om : locations){
            if(om.getId() == stopid){
                correct_monitor = locations.indexOf(om);
            }
        }
        boolean reserved = false;
        OwnMonitor om;
        location_lock.lock();
        try{
            om = locations.get(correct_monitor);
        }finally{
            location_lock.unlock();
        }
        if(correct_monitor != -1) {
           reserved = om.reserve(train_id);
        }
        return reserved;
    }

    /**
     * Gibt ein Gleis frei
     * @param gleisid
     * @param train_id
     */
    public void UnlockGleis(int gleisid, int train_id){
        int correct_gleis = -1;
        for(GleisMonitor gm : gleise){
            if(gm.getId() == gleisid){
                correct_gleis = gleise.indexOf(gm);
            }
        }
        GleisMonitor gm;
        gleise_lock.lock();
        try{
            gm = gleise.get(correct_gleis);
        }finally {
            gleise_lock.unlock();
        }
        if(correct_gleis != -1){
            gm.free_track(train_id);
        }

        waitforfdl_lock.lock();
        try{
            new_gleis_frei = true;
            Gleis_frei.signal();
        }finally{
            waitforfdl_lock.unlock();
        }

    }

    /**
     * Gibt einen Platz frei
     * @param stopid    id der Location
     * @param train_id  id des Zuges
     */
    public void FreePlace(int stopid, int train_id){
        int correct_monitor = -1;
        for(OwnMonitor om : locations){
            if(om.getId() == stopid){
                correct_monitor = locations.indexOf(om);
            }
        }
        OwnMonitor om;
        location_lock.lock();
        try{
            om = locations.get(correct_monitor);
        }finally{
            location_lock.unlock();
        }
        if(correct_monitor != -1) {
            om.free_space(train_id);
        }


        waitforfdl_lock.lock();
        try{
            new_gleis_frei = true;
            Gleis_frei.signalAll();
        }finally{
            waitforfdl_lock.unlock();
        }

    }

    /**
     * gibt an ob alle Trains regelgemäß terminiert sind
     * @return ob alle trains terminiert sind
     */
    public boolean checkDone(){
        boolean done = false;
        arrived_trains_lock.lock();
        try{
            if(arrived_trains == num_trains){
                done = true;
            }
        }finally{
            arrived_trains_lock.unlock();
        }
        return done;

    }

    /**
     * von Zug aufgerufen um zu signalisieren dass er fertig ist
     */
    public void isFinished(){
        arrived_trains_lock.lock();
        try{
            arrived_trains++;
        }finally{
            arrived_trains_lock.unlock();
        }
    }

    public Lock getWaitforfdl_lock() {
        return waitforfdl_lock;
    }

    public Condition getGleis_frei() {
        return Gleis_frei;
    }
}
