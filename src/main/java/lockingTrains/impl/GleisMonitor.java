package lockingTrains.impl;

public class GleisMonitor {

    private boolean reserved;       //gibt an ob das Gleis gerade reserved ist
    private int id;                 //id des Gleises für die Totale Ordnung
    private int train_id;           //id des Zuges der reserviert hat; -1 = kein Zug reserviert

    public GleisMonitor(int id){
        this.id = id;
        this.train_id = -1;
        this.reserved = false;
    }

    /**
     * Versucht einen Platz zu reservieren.
     * @param train_id  Id des aufrufenden damit man nachher nachvollziehen kann wer was reserviert hat.
     * @return true wenn reserviert wurde, false wenn es gescheitert ist.
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

    synchronized void reserveblocking(int train_id){

    }

    /**
     * Gibt das Gleis wieder frei
     * @param train_id Kann nur freigebn wenn es auch der Zug ist der es reserviert hat
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
}
