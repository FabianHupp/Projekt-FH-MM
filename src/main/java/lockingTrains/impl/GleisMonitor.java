package lockingTrains.impl;

public class GleisMonitor {

    private boolean reserved;       //Gleis reserviert?
    private int train_id;           //Reservierender Zug
    private int id, totalid;                 //Gleis-id
    private boolean einfahrt;               //Einfahrt?

    public GleisMonitor(int i, int totali, boolean einf){
        reserved = false;
        einfahrt = einf;
        totalid = totali;
        train_id = -1;
        id = i;
    }

    /**
     * Versucht einen Platz zu reservieren.
     * @param train_id  Id des Aufrufenden.
     * @return true wenn reserviert wurde, false wenn nicht.
     */
    synchronized boolean reserve(int train_id){
        if(reserved){
            return this.train_id == train_id;
        }else {
            this.train_id = train_id;
            reserved = true;
            return true;
        }
    }

    /**
     * Gibt das Gleis wieder frei.
     * @param train_id Nur haltender Zug kann freigeben.
     */
    synchronized void free_track(int train_id){
        if(this.train_id == train_id){
            reserved = false;
            this.train_id = -1;
        }
    }


    synchronized int getId(){
        return id;
    }

    synchronized int getTotalid() {return totalid;}

    synchronized boolean getIsEinfahrt(){return einfahrt;}
}
