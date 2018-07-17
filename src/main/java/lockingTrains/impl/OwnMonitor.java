package lockingTrains.impl;

public class OwnMonitor {

    int capacity;   //gibt die maximale Kapazität an; -1 = unendlich viel Platz
    int reserved = 0;   //gibt die aktuell belgten/reservierten Plätze an: reserved <= capacity
    int id;

    public OwnMonitor (int cap, int id){
        this.capacity = cap;
        this.id = id;
    }

    /**
     * Versucht einen Platz im Bahnhof zu reservieren;
     * @return gibt ein bool zurück das angibt ob das Reservieren erfolgreich war oder nicht
     */
    synchronized boolean reserve(){
        if(capacity == -1){
            reserved++;
            return true;
        }

        if(reserved < capacity){
            reserved++;
            return true;
        }

        return false;
    }

    /**
     * Gibt wieder einen Parkplatz frei, wenn überhaupt ein Zug im Bahnhof steht.
     * @return
     */
    synchronized void free_space(){
        if(reserved > 0){
            reserved--;
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

}
