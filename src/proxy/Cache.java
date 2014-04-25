package proxy;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
    
    private int BUF_SIZE = 8096;
    
    private String hostName;
    
    /**
     * 
     * Constructor
     * 
     * @param data
     * @param date
     */
    public Cache(long maxAge, long maxStale, String fileName, String hostName) {
        this.maxAge = maxAge;
        this.maxStale = maxStale;
        this.fileName = fileName.substring(4, hostName.length() - 4);
        //StringBuilder sb = new StringBuilder("");
        
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
        return fileName;
    }
    
    /**
     * 
     * TODO: Store Headers
     * @param dataTest 
     * @param map 
     * 
     * @param uri
     */
   // public void writeData(byte[] data) {
    public void writeData(InputStream istream, List<HTTPHeader> headerList, ByteArrayOutputStream dataTest) {
      
        byte buffer[]  = new byte[BUF_SIZE]; 
        int count; 
        FileOutputStream file = null;
        //ByteArrayInputStream in = new ByteArrayInputStream(ostream.toByteArray());
        
        
         try {
            file = new FileOutputStream(fileName);
          
           // for (HTTPHeader header : headerList) {
             //   if (header.getHeaderName() == null)
                    //file.write(header.toString().getBytes());
               //     continue;
                //else if (header.getHeaderName().equals("Content-Type"))
                  //  file.write(header.toString().getBytes());
                //else if (header.getHeaderName().equals("Content-Length"))
                  //  file.write(header.toString().getBytes());
            //}
            
            //file.write("\n".getBytes());
            //file.write("Content-Type: text/html; charset=utf-8\n\n".getBytes());
            while ( (count = istream.read(buffer, 0, BUF_SIZE)) > -1) {
                file.write(buffer, 0, count);
            }
            //file.write(dataTest.toString().getBytes());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
