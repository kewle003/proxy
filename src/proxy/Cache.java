package proxy;

import java.util.Date;

/**
 * 
 * This class handles a Cache object.
 * It will hold onto the data of the cache
 * and the age of the cache.
 * 
 * @author mark
 *
 */
public class Cache {
    
    //The html data
    private String data;
    //The expiration date
    private Date date;
    
    /**
     * 
     * Constructor
     * 
     * @param data
     * @param date
     */
    public Cache(String data, Date date) {
        this.data = data;
        this.date = date;
    }
    
    public void setDate(Date newDate) {
        date = newDate;
    }
    
    public void setData(String newData) {
        data = newData;
    }
    
    public String getData() {
        return data;
    }
    
    public Date getDate() {
        return date;
    }
    
    /**
     * 
     * This method checks if the cache data
     * has expired.
     * 
     * @param c
     * @return true - it has expired, false - it has not
     */
    public boolean isExpired(Cache c) {
        if (c.getDate().after(this.getDate())) {
            return true;
        } else {
            return false;
        }
    }
    
    

}
