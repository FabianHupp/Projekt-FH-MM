package lockingTrains.impl;

import lockingTrains.shared.Connection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class FahrtdienstLeitung {

    private Lock gleise_lock, location_lock, arrived_trains_lock;
    private int num_trains, arrived_trains;
    private List<OwnMonitor> locations;
    private List<GleisMonitor> gleise;


    FahrtdienstLeitung(List<OwnMonitor> loc, List<GleisMonitor> gle, int num_tra){
        arrived_trains_lock = new ReentrantLock();
        location_lock = new ReentrantLock();
        gleise_lock = new ReentrantLock();
        num_trains = num_tra;
        arrived_trains = 0;
        locations = loc;
        gleise = gle;
    }



    //-------------- Gleis-Methoden:

    /**
     * Lockt ein Gleis.
     * @param gleisid  Gleisid
     * @param train_id Zugid
     * @return true wenn reseviert wurde, false wenn schon belegt
     */
    public boolean lockGleis(int gleisid, int train_id) {
        GleisMonitor gm = getGleisMonitor(gleisid);
        return gm.reserve(train_id);
    }

    public void lock_Gleis_block(int gleisid, int train_id){
        GleisMonitor gm = getGleisMonitor(gleisid);
        gm.reserve_blocking(train_id);
    }

    /**
     * Gibt ein Gleis frei.
     * @param gleisid   freizugebendes Gleis
     * @param train_id  zug der freigibt
     */
    public void UnlockGleis(int gleisid, int train_id) {
        GleisMonitor gm = getGleisMonitor(gleisid);
        gm.free_track(train_id);
    }



    //-------------- Parkplatz-Methoden:

    /**
     * Reserviert einen ParkPlatz in einem Stop.
     * @param stopid   Locationid
     * @param train_id Zugid
     * @return true wenn es geklappt hat, false wenn nicht
     */
    public boolean ReservePlace(int stopid, int train_id) {
        OwnMonitor om = getLocationMonitor(stopid);
        return om.reserve(train_id);
    }

    /**
     * Gibt einen ParkPlatz frei.
     * @param stopid   id der Location
     * @param train_id id des Zuges
     */
    public void FreePlace(int stopid, int train_id) {
        OwnMonitor om = getLocationMonitor(stopid);
        om.free_space(train_id);
    }



    //--------------  Ein-/Durchfahrtsgleis-Methoden:

    /**
     * Reserviert das Ein-/Durchfahrtsgleis eines Stops.
     * @param stopid   stop an dem reserviert wird
     * @param train_id zug der reservieren will
     * @return true wenn reservieren geklappt hat, false falls wenn nicht
     */
    public boolean reserve_Einfahrt(int stopid, int train_id) {
        GleisMonitor em = getEinfahrtMonitor(stopid);
        return em.reserve(train_id);
    }

    public void reserve_Einfahrt_block(int stopid, int train_id){
        GleisMonitor em = getEinfahrtMonitor(stopid);
        em.reserve_blocking(train_id);
    }

    /**
     * Gibt das Ein-/Durchfahrtsgleis eines Stops wieder frei.
     * @param stopid   stop der freigegeben werden soll
     * @param train_id zug der freigibt
     */
    public void free_Einfahrt(int stopid, int train_id) {
        GleisMonitor em = getEinfahrtMonitor(stopid);
        em.free_track(train_id);
    }



    //-------------- Finished-Methoden:

    /**
     * Gibt an ob alle Trains regelgemäß terminiert sind
     * @return ob alle trains terminiert sind
     */
    public boolean checkDone() {
        boolean done = false;
        arrived_trains_lock.lock();
        try {
            if (arrived_trains == num_trains) {
                done = true;
            }
        } finally {
            arrived_trains_lock.unlock();
        }
        return done;

    }

    /**
     * Von Zug aufgerufen um zu signalisieren dass er fertig ist
     */
    public void isFinished() {
        arrived_trains_lock.lock();
        try {
            arrived_trains++;
        } finally {
            arrived_trains_lock.unlock();
        }
    }



    //-------------- Hilfsmethoden:

    /**
     * Hilfsmethode um Index des richtigen Stops in der Stop-Liste zu finden.
     * @param stopid zu findender stop
     * @return Monitor der Location in Liste
     */
    public OwnMonitor getLocationMonitor(int stopid) {
        OwnMonitor m;
        int correct_monitor = -1;
        for (OwnMonitor om : locations) {
            if (om.getId() == stopid) {
                correct_monitor = locations.indexOf(om);
            }
        }
        location_lock.lock();
        try {
            m = locations.get(correct_monitor);
        } finally {
            location_lock.unlock();
        }
        return m;
    }

    /**
     * Hilfsmethode um Index des richtigen Stops in Gleis-Liste zu finden.
     * @param gleisid zu findendes gleis
     * @return Monitor des Gleises in Liste
     */
    public GleisMonitor getGleisMonitor(int gleisid) {
        int correct_gleis = -1;
        for (GleisMonitor gm : gleise) {
            if(!gm.getIsEinfahrt()){
                if (gm.getId() == gleisid) {
                    correct_gleis = gleise.indexOf(gm);
                }
            }
        }
        GleisMonitor m;
        gleise_lock.lock();
        try {
            m = gleise.get(correct_gleis);
        } finally {
            gleise_lock.unlock();
        }
        return m;
    }

    /**
     * Hilfsmethode um Index der richtigen Einfahrt in Einfahrt-Liste zu finden.
     * @param einfahrtid zu findende Einfahrt
     * @return Monitor der Einfahrt in Liste
     */
    public GleisMonitor getEinfahrtMonitor(int einfahrtid){
        int correct_gleis = -1;
        for(GleisMonitor gm : gleise){
           if(gm.getIsEinfahrt()){
               if(gm.getId() == einfahrtid){
                   correct_gleis = gleise.indexOf(gm);
               }
           }
        }
        GleisMonitor m;
        gleise_lock.lock();
        try{
            m = gleise.get(correct_gleis);
        }finally{
            gleise_lock.unlock();
        }
        return m;
    }


    public List<GleisMonitor> getRoute(List<Connection> route){
        List<GleisMonitor> ret = new ArrayList<>();
        for(Connection c : route){
            ret.add(getGleisMonitor(c.id()));
            ret.add(getEinfahrtMonitor(c.first().id()));
            ret.add(getEinfahrtMonitor(c.second().id()));
        }
        return ret;
    }
}
