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
    public void writeData(String uri) {
        if (!uri.startsWith("http:")) {
            uri = "http://".concat(uri);
        }
        URL u;
        InputStream istream = null;
        try {
            u = new URL(uri);
            URLConnection conn = u.openConnection();
            istream = conn.getInputStream();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (istream == null)
            return;
        
        BufferedReader inLine = new BufferedReader(new InputStreamReader(istream));
        ByteArrayOutputStream dataBuf = new ByteArrayOutputStream(BUF_SIZE);
        PrintWriter dataWriter = new PrintWriter( dataBuf );
        String response = new String("");
        
        try {
            
            response = inLine.readLine();
            if (response != null) {
                while ((response = inLine.readLine()) != null) {
                    dataWriter.println(response);
                    response = inLine.readLine();
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataWriter.flush();
        System.out.println("----------BEGIN CACHED VALUE---------");
        System.out.println(dataBuf.toString());
        System.out.println("----------END CACHED VALUE---------");
        data = dataBuf.toString().getBytes();
        
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
