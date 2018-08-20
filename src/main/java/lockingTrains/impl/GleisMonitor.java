package lockingTrains.impl;

public class GleisMonitor {

    private boolean reserved;       //Gleis reserviert?
    private int train_id;           //Reservierender Zug
    private int id;                 //Gleis-id

    public GleisMonitor(int id){
        this.reserved = false;
        this.train_id = -1;
        this.id = id;
    }

    /**
     * Versucht einen Platz zu reservieren.
     * @param train_id  Id des Aufrufenden.
     * @return true wenn reserviert wurde, false wenn nicht.
     */
    synchronized boolean reserve(int train_id){
        if(reserved){
            if(this.train_id == train_id){
                reserved = true;
                return true;
            }
            return false;
        }
        this.train_id = train_id;
        reserved = true;
        return true;
    }

    /**
     * Reserviert Gleis wartend.
     * @param train_i Id des Reservierenden.
     */
    synchronized void reserveblocking(int train_i){
        if(reserved && !(train_i == train_id)){
            while(true){
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(!reserved) {
                    reserved = true;
                    train_id = train_i;
                    return;
                }
            }
        }
        train_id = train_i;
        reserved = true;
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
        notifyAll();
    }


    synchronized int getId(){
        return id;
    }
}
