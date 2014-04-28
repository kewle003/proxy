package proxy;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

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
    private String hostName;
    private long maxAge;
    private long maxStale;
    private String fileName;
    private String filePath;
    private int BUF_SIZE = 8321;
    
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
        this.hostName = hostName;
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
     * @param logger 
     * @param dataTest 
     * @param map 
     * 
     * @param uri
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
    
    public void writeData(InputStream istream, Logger logger) {
        try {
            logger.info(hostName+"::"+filePath);
            BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(filePath));
            //BufferedReader inLine = new BufferedReader(new InputStreamReader(inputStream));
            byte[] buffer = new byte[BUF_SIZE];
            int count;
            
            while ((count = istream.read(buffer, 0, BUF_SIZE )) > -1) {
                writer.write(buffer, 0, count);
            }
            writer.flush();
            writer.close();
            istream.close();
            //inLine.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
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
