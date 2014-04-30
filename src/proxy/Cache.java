package proxy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

/**
 * 
 * This class handles a Cache object.
 * It will hold onto the data of the cache
 * and the age of the cache. It handles all
 * Caching.
 * 
 * @author mark
 *
 */
public class Cache {
    
    //The expiration date
    private String hostName;
    
    //The max-age Cache-Control value
    private long maxAge;
    
    //The max-stale Cache-Control value
    private long maxStale;
    
    //The cache file name
    private String fileName;
    
    //The path ProxyServer/host/fileName
    private String filePath;
    
    /**
     * 
     * Constructor, takes in three values specified
     * by the Cache-Control header.
     * 
     * @param maxAge - specified by max-age field in milliseconds
     * @param maxStale - specified by max-stale field in milliseconds
     * @param hostName - specified by HTTPRequest object's hostName field
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
        this.hostName = hostName;
    }
    
    /**
     * 
     * Retrieves the max-age
     * header value.
     * 
     * @return long - max-age value
     */
    public long getMaxAge() {
        return maxAge;
    }
    
    /**
     * 
     * Retrieves the max-stale
     * header value.
     * 
     * @return long - max-stale value
     */
    public long getMaxStale() {
        return maxStale;
    }
    
    /**
     * 
     * This method will fetch the
     * path to the Cached file.
     * 
     * @return - String filePath to the Cached object
     */
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * 
     * This method will write the
     * data in String format to the cached
     * file in ProxyServerCache/hostName directory.
     * 
     * @param data - The data from HTTPResponse object
     * @param logger - Used for debugging
     */
    public void writeData(String data, Logger logger) {
        try {
            logger.info(hostName+"::"+filePath);
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
     * has expired. It does so by checking
     * the max-age against the current time. Furthermore
     * if max-stale is specified it will add this to the
     * max-age value.
     * 
     * @return true - it has expired, false - it has not
     */
    public boolean isExpired() {
        long val = System.currentTimeMillis();
        System.out.println("Comparing: " +val+" to the maxAge: " + maxAge+ " and the maxStale: " +maxStale);
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
