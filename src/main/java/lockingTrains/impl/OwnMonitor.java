package lockingTrains.impl;

import java.util.ArrayList;
import java.util.List;

public class OwnMonitor {

    private int capacity, reserved, lockedid, id;
    private List<Integer> train_ids;
    private boolean locked;

    public OwnMonitor (int cap, int i){
        train_ids = new ArrayList<>();
        locked = false;
        capacity = cap;
        lockedid = -1;
        reserved = 0;
        id = i;
    }

    /**
     * Reserviert das Gleis für die einfahrenden/durchfahrenden Züge.
     * @param train_id  Id des reservierenden Zuges.
     * @return  Reservierung geklappt?
     */
    synchronized boolean reserve_arrive(int train_id){
            if(locked){
                return lockedid == train_id;
            }else{
                locked = true;
                lockedid = train_id;
                return true;
            }
    }

    /**
     * Reserviert das Gleis für die einfahrenden/durchfahrenden Züge blockierend.
     * @param traind_id Id des reservierenden Zuges.
     */
    synchronized void reserve_arrive_blocking(int train_i){
        while(locked && !(train_i == lockedid)){
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
        lockedid = train_i;
        locked = true;
    }

    /**
     * Gibt das Ein-/Durchfahrtsgleis frei.
     * @param train_id  Id des Freigebenden.
     */
    synchronized void free_arrive(int train_id){
                if(train_id == lockedid){
                    locked = false;
                    lockedid = -1;
                    notifyAll();
                }
    }


    /**
     * Versucht einen ParkPlatz im Bahnhof/Knotenpunkt zu reservieren.
     * @param train_id Id des reservierenden Zuges.
     * @return Reservierung geklappt?
     */
    synchronized boolean reserve(int train_id){
        if(train_ids.contains(train_id)){
            return true;
        }
        if(capacity == -1){
            reserved++;
            train_ids.add(train_id);
            return true;
        }
        if(reserved < capacity){
            reserved++;
            train_ids.add(train_id);
            return true;
        }
        return false;
    }

    /**
     * Gibt einen Parkplatz frei.
     * @param train_id  Id des verlassenden Zuges.
     */
    synchronized void free_space(int train_id){
        if(reserved > 0){
           if(train_ids.contains(train_id)){
                reserved--;
                int a = -1;
                for(Integer in : train_ids){
                    if(in == train_id){
                        a = train_ids.indexOf(in);
                    }
                }
                if(a != -1){
                    train_ids.remove(a);
                }

           }
        }
    }

    synchronized int getId(){return this.id;}

    synchronized int getCapacity(){return this.capacity; }
}
