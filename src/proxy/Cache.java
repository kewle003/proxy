package proxy;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

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
    
    
    //The expiration date
    private long maxAge;
    private long maxStale;
    private String fileName;
    private String filePath;
    
    /**
     * 
     * Constructor
     * 
     * @param data
     * @param date
     */
    public Cache(long maxAge, long maxStale, String hostName) {
        this.maxAge = maxAge;
        this.maxStale = maxStale;
        this.fileName = hostName.substring(4, hostName.length() - 4)+ ".html"; 
        File f = new File(System.getProperty("user.dir") + "/ProxyServerCache/" +hostName);
        if (!f.exists()) {
            f.mkdir();
        }
        filePath = System.getProperty("user.dir") + "/ProxyServerCache/" +hostName+ "/" +fileName;
        
    }
    
    public void setMaxAge(long newMaxAge) {
        maxAge = newMaxAge;
    }
    
    public void setMaxStale(long newMaxStale) {
        maxStale = newMaxStale;
    }
    
    
    public long getMaxAge() {
        return maxAge;
    }
    
    public long getMaxStale() {
        return maxStale;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * 
     * @param dataTest 
     * @param map 
     * 
     * @param uri
     */
    public void writeData(String data) {
        try {
            PrintWriter writer = new PrintWriter(filePath, "UTF-8");
            writer.println(data);
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        
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
