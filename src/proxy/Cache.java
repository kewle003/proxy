package proxy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
    private byte[] data;
    //The expiration date
    private long maxAge;
    private long maxStale;
    
    private int BUF_SIZE = 8096;
    
    /**
     * 
     * Constructor
     * 
     * @param data
     * @param date
     */
    public Cache(long maxAge, long maxStale) {
        this.maxAge = maxAge;
        this.maxStale = maxStale;
    }
    
    
    public void setData(byte[] newData) {
        data = newData;
    }
    
    public void setMaxAge(long newMaxAge) {
        maxAge = newMaxAge;
    }
    
    public void setMaxStale(long newMaxStale) {
        maxStale = newMaxStale;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public long getMaxAge() {
        return maxAge;
    }
    
    public long getMaxStale() {
        return maxStale;
    }
    
    /**
     * 
     * TODO: Store Headers
     * 
     * @param uri
     */
    public void writeData(byte[] data) {
        //this.data = new data[data.length];
        this.data = data;  
    }
    
    /**
     * 
     * This method checks if the cache data
     * has expired.
     * 
     * @param c
     * @return true - it has expired, false - it has not
     */
    public boolean isExpired() {
        long val = System.currentTimeMillis();
        if (maxStale != -1) {
            long temp = maxAge + maxStale;
            if (temp > val) {
                return false;
            }
        } else {
            if (maxAge > val) {
                return false;
            }
        }
        return true;
    }

    
    

}
