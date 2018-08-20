package lockingTrains.impl;

import java.util.ArrayList;
import java.util.List;

public class OwnMonitor {

    private int capacity;   //gibt die maximale Kapazität an parkplätzen; -1 = unendlich viel Platz
    private int reserved = 0;   //gibt die aktuell belgten/reservierten ParkPlätze an: reserved <= capacity
    private boolean locked;
    private int lockedid;
    private int id;
    private List<Integer> train_ids;
    private boolean ist_Parkplatz;


    public OwnMonitor (int cap, int id, boolean station_type){
        this.capacity = cap;
        this.id = id;
        train_ids = new ArrayList<>();
        ist_Parkplatz = station_type;
        locked = false;
        lockedid = -1;
    }

    /**
     * Reserviert das Gleis für die einfahrenden/durchfahrenden Züge
     * @param train_id
     * @return
     */
    synchronized boolean reserve_arrive(int train_id){
            if(locked){
                if(lockedid == train_id){
                    return true;
                }else{
                    return false;
                }
            }

            if(!locked){
                locked = true;
                lockedid = train_id;
                return true;
            }

            return false;
    }

    synchronized void reserve_arrive_blocking(int traind_id){

    }

    synchronized void free_arrive(int train_id){
                if(train_id == lockedid){
                    locked = false;
                    train_id = -1;
                }
    }


    /**
     * Versucht einen ParkPlatz im Bahnhof/Knotenpunkt zu reservieren;
     * @return gibt ein bool zurück das angibt ob das Reservieren erfolgreich war oder nicht
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
     * Gibt wieder einen Parkplatz frei, wenn überhaupt ein Zug im Bahnhof steht.
     * @return
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

    /**
     * Returns the Id of the Monitor.
     * @return id
     */
    synchronized int getId(){
        return this.id;
    }

    synchronized int getCapacity() { return this.capacity; }

    public boolean getIst_Knotenpunkt() {
        return ist_Parkplatz;
    }
}
