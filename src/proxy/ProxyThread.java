package proxy;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * 
 * This class handles handshaking between
 * a web browser and a web server. The class
 * is responsible for building Requests, Responses, and
 * Cache control.
 * 
 * @author mark
 *
 */
public class ProxyThread extends Thread {
    
    //A handle on our web browser socket
    protected Socket clientSocket;
    
    //A handle on our configuration file
    protected ConfigFile configFile;
    
    //HashMap to our cache, key - httpHost, value - Cache object
    private static HashMap<String, Cache> cache = new HashMap<String, Cache>();
    
    //A handle on our logger
    private static Logger logger = Logger.getLogger("MyLogger");
    static {
        try {
            FileHandler fileHandler = new FileHandler("loggerFile.txt");
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //Default buffer size
    private int BUF_SIZE = 8192;
   
    //Used for debugging
    public static int THREAD_COUNT = 0;
    
    //Handle on our Request object
    private HTTPRequest httpReq;
    
    //Handle on our response object
    private HTTPResponse httpResp;
    
    /**
     * 
     * Default constructor that takes in the
     * socket from the browser and a Logger
     * for debugging purposes.
     * 
     * @param clientSocket
     * @param logger
     */
    public ProxyThread(Socket clientSocket) {
        super("ProxyThread");
        this.clientSocket = clientSocket;
        //this.logger = logger;
        THREAD_COUNT++;

    }
    
    /**
     * 
     * This method will handle handshaking
     * from a client to a server.
     * 
     */
    public synchronized void run() {  
        
       if (clientSocket.isConnected()) {
           try {
               //Create a new HTTPRequest object
               httpReq = new HTTPRequest(clientSocket);
               //Verify a valid URL was asked for
               if (httpReq.getURI() == null) {
                   return;
               }
                            
               //Set the disAllowed MIME types for the Request.
               httpReq.setDisAllowedMIME(ProxyServer.getConfigFile().getDissallowedDomains().get(httpReq.getHost()));
               
               //Verify if we have a blocked site
               if (httpReq.getDisAllowedMIME().contains("all")) {
                   logger.info(httpReq.getHost()+"::request for blocked site");
                   writeResponse(getBlockedSiteScreen(httpReq.getURI()), httpReq.getClientSocket().getOutputStream());
                   httpReq.getClientSocket().close();
               } else {
                  // System.out.println(httpReq.getMethod()+ " " +httpReq.getURI()+ " " +httpReq.getProtocol());

                   //Create a new HTTPResponse object
                   httpResp = new HTTPResponse();
                   //Parse the Response
                   httpResp.parseResponse(httpReq);
                   
                   //Do we have a valid status code?
                   if (httpResp.getHttpUrlConnection().getResponseCode() >= 400) {
                       Socket s = new Socket(httpReq.getHost(), httpReq.getPort());
                       s.getOutputStream().write(httpReq.getData().getBytes());
                       writeResponse(s.getInputStream(), httpReq.getClientSocket().getOutputStream());
                       s.close();
                       return;
                   }
                   
                   
                   //Check if Content-Type is an image
                   if (httpResp.isImage()) {
                       //Check if Content-Type is valid
                       if (httpResp.isInValidContent()) {
                           logger.info(httpReq.getHost()+"::requesting for bad content");
                           writeResponse(getErrorMessage("This image is not Allowed!"), httpReq.getClientSocket().getOutputStream());
                       } else {
                           writeResponse(httpResp.getHttpUrlConnection().getInputStream(), httpReq.getClientSocket().getOutputStream());
                       }
                   } else {
                       //Can we Cache the Response?
                       if (httpResp.isCacheAble() || httpReq.onlyIfCacheSet()) {
                           //Does it already exist? Furthermore, is only-if-cache not set?
                           if (cache.containsKey(httpReq.getHost())) {
                               Cache cacheToCheck = cache.get(httpReq.getHost());
                               //If so has it expired?
                               if (cacheToCheck.isExpired()) {
                                   logger.info(""+httpReq.getHost()+"::expired cache, rewriting cache");
                                   Cache newCache = null;
                                   //Set max-age and max-stale
                                   if (httpResp.getMaxAgeCache() > 0) {
                                       if (httpResp.getMaxStaleCache() > 0) {
                                          newCache = new Cache(httpResp.getMaxAgeCache()+System.currentTimeMillis(), httpResp.getMaxStaleCache()+System.currentTimeMillis(), httpReq.getHost());
                                       }
                                       newCache = new Cache(httpResp.getMaxAgeCache()+System.currentTimeMillis(), 0, httpReq.getHost());
                                   } else {
                                       if (httpResp.getMaxStaleCache() > 0) {
                                           newCache = new Cache(0, httpResp.getMaxStaleCache()+System.currentTimeMillis(), httpReq.getHost());
                                       } else {
                                           newCache = new Cache(0, 0, httpReq.getHost());
                                       }
                                   }
                                   newCache.writeData(httpResp.getData(), logger);
                                   //Rewrite the cache
                                   cache.put(httpReq.getHost(), newCache);
                                   writeResponseFromCache(newCache.getFilePath(), httpReq.getClientSocket().getOutputStream(), httpReq.getHost());
                               } else {
                                   if (httpReq.onlyIfCacheSet())
                                       logger.info(httpReq.getHost()+"::only-if-cache specified pulling from cache");
                                   //Write the cache data
                                   writeResponseFromCache(cacheToCheck.getFilePath(), httpReq.getClientSocket().getOutputStream(), httpReq.getHost());
                               }
                           } else {
                               logger.info(httpReq.getHost()+"::attmpting to create cache");
                               
                               Cache newCache = null;
                               
                               if (httpReq.onlyIfCacheSet()) {
                                   logger.info(httpReq.getHost()+"::on-if-cache specified, no cache existed!");
                                   writeResponse(getErrorMessageWithResponse("HTTP/1.1 504 Gateway Timeout"),httpReq.getClientSocket().getOutputStream());
                                   httpReq.getClientSocket().close();
                                   httpResp.getHttpUrlConnection().disconnect();
                                   return;
                               }
                               
                               //Set max-age and max-stale
                               if (httpResp.getMaxAgeCache() > 0) {
                                   if (httpResp.getMaxStaleCache() > 0) {
                                      newCache = new Cache(httpResp.getMaxAgeCache()+System.currentTimeMillis(), httpResp.getMaxStaleCache()+System.currentTimeMillis(), httpReq.getHost());
                                   }
                                   newCache = new Cache(httpResp.getMaxAgeCache()+System.currentTimeMillis(), 0, httpReq.getHost());
                               } else {
                                   if (httpResp.getMaxStaleCache() > 0) {
                                       newCache = new Cache(0, httpResp.getMaxStaleCache()+System.currentTimeMillis(), httpReq.getHost());
                                   } else {
                                       newCache = new Cache(0, 0, httpReq.getHost());
                                   }
                               }       
                               newCache.writeData(httpResp.getData(), logger);
                               //Create a brand new Cache
                               cache.put(httpReq.getHost(), newCache);
                               writeResponseFromCache(newCache.getFilePath(), httpReq.getClientSocket().getOutputStream(), httpReq.getHost()); 
                           }
                       } else {
                           logger.info(httpReq.getHost()+"::contacted orgin server");
                           //If we reached here, we have an uncacheable object
                           //Check if we wish to grab from cache if response takes to long
                           if (httpResp.isText())
                               writeResponse(httpResp.getData().getBytes(), httpReq.getClientSocket().getOutputStream());
                           else
                               writeResponse(httpResp.getHttpUrlConnection().getInputStream(), httpReq.getClientSocket().getOutputStream());
                       }
                   }
                   
               }

           } catch (IOException e) {
               logger.info("Exception occured!! Writing Error Message");
               try {
                writeResponse(getErrorMessage("Exception occured!! Writing Error Message"), clientSocket.getOutputStream());
               } catch (IOException e1) {
                   logger.info("Exception Occured! Tried to write errormessage but Pipe Broken!");
               }
            } finally {
                //Close up our sockets.
                if (httpReq != null)
                    try {
                        httpReq.getClientSocket().close();
                    } catch (IOException e1) {
                    }
                if (httpResp != null)
                   if (httpResp.getHttpUrlConnection() != null)
                       httpResp.getHttpUrlConnection().disconnect();
            } 
       }
       
    } 

    /**
     * 
     * This will write a response from the Cached file
     * 
     * @param filePath - where the Cache file is located
     * @param outputStream - the client's output stream
     * @param hostName - the host
     */
    private void writeResponseFromCache(String filePath, OutputStream outputStream, String hostName) {
        DataOutputStream out = new DataOutputStream(outputStream);
        logger.info(""+hostName+"::"+filePath+ " served from cache");
        try {
            BufferedInputStream reader = new BufferedInputStream(new FileInputStream(filePath));
            byte[] buffer = new byte[BUF_SIZE];
            int count;
            while ((count = reader.read(buffer, 0, BUF_SIZE)) > -1) {
                out.write(buffer, 0, count);
            }
            out.flush();
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * This method writes data from the server to the
     * client.
     * 
     * @param bytes
     * @param ostream
     * @param onlyIfCache 
     */
    private void writeResponse(byte[] bytes, OutputStream ostream) {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        DataOutputStream out = new DataOutputStream(ostream);
        int count;
        byte[] buffer = new byte[BUF_SIZE];
        try {
            
            while ((count = in.read(buffer, 0, BUF_SIZE)) > -1) {
                out.write(buffer, 0, count);
            }
            out.flush();
            in.close();;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 
     * This method simply writes the bytes from the
     * Server's socket to the Client's socket.
     * 
     * @param istream
     * @param ostream
     */
    public synchronized void writeResponse(InputStream istream, OutputStream ostream) {
        //Client's ostream
        DataOutputStream out = new DataOutputStream(ostream);
        byte buffer[]  = new byte[BUF_SIZE]; 
        int count; 

         try {
            while ( (count = istream.read( buffer, 0, BUF_SIZE)) > -1)  {
                  out.write(buffer, 0, count); 
             }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
         
    }
    
    /**
     * 
     * This will display a blocked site.
     * 
     * @param uri - The URL that caused the block
     * @return byte[] - Returns the data in a byte array format
     */
    public byte[] getBlockedSiteScreen(String uri) {
        String data = "<html><head><meta http-equiv=\"content-type\" content=\"text/html; "
                + "charset=ISO-8859-1\"></head><body><h1>Access Forbidden!</h1>"
                + "<h3><span style=\"color:red;\">"+uri+"</span> is a blocked site!</h3>"
                    + "</body></html>";
        StringBuilder errScreen = new StringBuilder("");
        errScreen.append("HTTP/1.1 403 Forbidden\r\n");
        errScreen.append("Content-Type: text/html\r\n");
        errScreen.append("Content-Length: ");
        errScreen.append(data.length());
        errScreen.append("\r\n\r\n");
        errScreen.append(data);
        return errScreen.toString().getBytes();
    }
    
    /**
     * 
     * This may be used to block sites that contain
     * a disallowed MIME type or Error's that occurred
     * from the ProxyServer
     * 
     * @param msg - String the error message to display
     * @return byte[] - Returns the data in a byte array format
     */
    public byte[] getErrorMessage(String msg) {
        String data = "<html><head><meta meta http-equiv=\"content-type\" content=\"text/html; "
                + "charset=ISO-8859-1\"></head><body>"+msg+"</body></html>";
        StringBuilder errScreen = new StringBuilder("");
        errScreen.append("HTTP/1.1 403 Forbidden\r\n");
        errScreen.append("Content-Type: text/html\r\n");
        errScreen.append("Content-Length: ");
        errScreen.append(data.length());
        errScreen.append("\r\n\r\n");
        errScreen.append(data);
        return errScreen.toString().getBytes();
    }
    
    /**
     * 
     * This will display the error with a 
     * response code and message
     * 
     * @param responseMessage - String like HTTP/1.1 403 Access Forbidden
     * @return byte[] - Returns the data in a byte array format
     */
    private byte[] getErrorMessageWithResponse(String responseMessage) {
        String data = "<html><head><meta meta http-equiv=\"content-type\" content=\"text/html; "
                + "charset=ISO-8859-1\"></head><body>Error Occured or Forbidden Content!</body></html>";
        StringBuilder errScreen = new StringBuilder("");
        errScreen.append(responseMessage+"\r\n");
        errScreen.append("Content-Type: text/html\r\n");
        errScreen.append("Content-Length: ");
        errScreen.append(data.length());
        errScreen.append("\r\n\r\n");
        errScreen.append(data);
        return errScreen.toString().getBytes();
    }
    
    
}