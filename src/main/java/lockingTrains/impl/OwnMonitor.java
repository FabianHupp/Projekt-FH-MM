package lockingTrains.impl;

import java.util.ArrayList;
import java.util.List;

public class OwnMonitor {

    private int capacity, reserved, id, totalid;
    private List<Integer> train_ids;

    public OwnMonitor (int cap, int i, int totali){
        train_ids = new ArrayList<>();
        totalid = totali;
        capacity = cap;
        reserved = 0;
        id = i;
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

    synchronized int getTotalid(){return this.totalid;}

    synchronized int getCapacity(){return this.capacity; }
}
