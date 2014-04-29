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
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * 
 * This class handles handshaking between
 * a web browser and a web server.
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
    private Logger logger;
    
    //Default buffer size
    private int BUF_SIZE = 8192;
   
    //Used for debugging
    public static int THREAD_COUNT = 0;
    
    //Handle on our host
    private String host;

    /**
     * 
     * Default constructor that takes in the
     * socket from the browser and a Logger
     * for debugging purposes.
     * 
     * @param clientSocket
     * @param logger
     */
    public ProxyThread(Socket clientSocket, Logger logger) {
        super("ProxyThread");
        this.clientSocket = clientSocket;
        this.logger = logger;
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
               System.out.println("Thread read!");
               //Create a new HTTPRequest object
               HTTPRequest httpReq = new HTTPRequest(clientSocket);
               //logger.info(httpReq.getHost());
               //Verify a valid URL was asked for
               if (httpReq.getURI() == null) {
                   return;
               }
               
               //Set the disAllowed MIME types for the Request.
               httpReq.setDissAllowedMIME(ProxyServer.getConfigFile().getDissallowedDomains().get(httpReq.getHost()));
               
               //Verify if we have a blocked site
               if (httpReq.getDissAllowedMIME().contains("all")) {
                   writeResponse(getBlockedSiteScreen(httpReq.getURI()), httpReq.getClientSocket().getOutputStream(), httpReq.onlyIfCacheSet(), httpReq.getHost());
                   httpReq.getClientSocket().close();
               } else {
                   //Create a new HTTPResponse object
                   HTTPResponse httpResp = new HTTPResponse();
                   //Parse the Response
                   httpResp.parseResponse(httpReq);
                   
                   //Do we have a valid status code?
                   if (httpResp.getHttpUrlConnection().getResponseCode() > 400) {
                       writeResponse(getErrorMessage(httpResp.getHttpUrlConnection().getResponseMessage()), httpReq.getClientSocket().getOutputStream(), httpReq.onlyIfCacheSet(), httpReq.getHost());
                       httpReq.getClientSocket().close();
                       return;
                   }
                   
                   //Check if Content-Type is an image
                   if (httpResp.isImage()) {
                       //Check if Content-Type is valid
                       if (httpResp.isInValidContent()) {
                           writeResponse(getErrorMessage(), httpReq.getClientSocket().getOutputStream(), httpReq.onlyIfCacheSet(), httpReq.getHost());
                       } else {
                           writeResponse(httpResp.getHttpUrlConnection().getInputStream(), httpReq.getClientSocket().getOutputStream(), httpReq.onlyIfCacheSet(), httpReq.getHost());
                       }
                   } else {
                       //Can we Cache the Response?
                       if (httpResp.isCacheAble()) {
                           //Does it already exist? Furthermore, is only-if-cache not set?
                           if (cache.containsKey(httpReq.getHost()) && !httpReq.onlyIfCacheSet()) {
                               Cache cacheToCheck = cache.get(httpReq.getHost());
                               //If so has it expired?
                               if (cacheToCheck.isExpired()) {
                                   //logger.info(""+httpReq.getHost()+"::expired");
                                   System.out.println("Expired for: " +httpReq.getHost());
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
                                   System.out.println("Cache not expired for: " +httpReq.getHost());
                                   //Write the cache data
                                   writeResponseFromCache(cacheToCheck.getFilePath(), httpReq.getClientSocket().getOutputStream(), httpReq.getHost());
                               }
                           } else {
                               System.out.println("Cache doesnt not exist: " +httpReq.getHost());
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
                               //Create a brand new Cache
                               cache.put(httpReq.getHost(), newCache);
                               writeResponseFromCache(newCache.getFilePath(), httpReq.getClientSocket().getOutputStream(), httpReq.getHost()); 
                           }
                       } else {
                           //logger.info(httpReq.getHost()+"::contacted orgin server");
                           //If we reached here, we have an uncacheable object
                           //Check if we wish to grab from cache if response takes to long
                           if (httpReq.onlyIfCacheSet())
                               httpResp.getHttpUrlConnection().setReadTimeout(5000);
                           if (httpResp.isText())
                               writeResponse(httpResp.getData().getBytes(), httpReq.getClientSocket().getOutputStream(), httpReq.onlyIfCacheSet(), httpReq.getHost());
                           else
                               writeResponse(httpResp.getHttpUrlConnection().getInputStream(), httpReq.getClientSocket().getOutputStream(), httpReq.onlyIfCacheSet(), httpReq.getHost());
                       }
                   }
                   //Close up our sockets.
                   httpReq.getClientSocket().close();
                   httpResp.getHttpUrlConnection().disconnect();
               }

           } catch (IOException e) {
               e.printStackTrace();
               System.out.println(host);
           } 
       }
       
    } 
    
    
    

  /*  private void writeResponseFromCache(String filePath, OutputStream outputStream, String hostName) {
        FileReader fr;
        DataOutputStream out = new DataOutputStream(outputStream);
        try {
            logger.info(""+hostName+"::"+filePath+ " served from cache");
            fr = new FileReader(filePath);
            BufferedReader buf = new BufferedReader(fr);
            String currentLine;
            while ((currentLine = buf.readLine()) != null) {
                out.writeBytes(currentLine);
            }
            out.flush();
            out.close();
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
    }*/
    
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
            out.close();
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
    private void writeResponse(byte[] bytes, OutputStream ostream, boolean onlyIfCache, String hostName) {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        DataOutputStream out = new DataOutputStream(ostream);
        int count;
        byte[] buffer = new byte[BUF_SIZE];
        try {
            
            while ((count = in.read(buffer, 0, BUF_SIZE)) > -1) {
                out.write(buffer, 0, count);
            }
            out.flush();
            in.close();
            out.close();
        } catch(SocketTimeoutException e) {
            try {
                out.close();
                in.close();
                if (onlyIfCache) {
                    if (cache.containsKey(hostName)) {  
                        writeResponseFromCache(cache.get(hostName).getFilePath(), ostream, hostName);
                    } else {
                        ostream.write(getErrorMessage("HTTP/1.1 504 Gateway Timeout\r\n"));
                        ostream.flush();
                        ostream.close();
                    }
                } else {
                    ostream.write(getErrorMessage("HTTP/1.1 504 Gateway Timeout\r\n"));
                    ostream.flush();
                    ostream.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            
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
    public synchronized void writeResponse(InputStream istream, OutputStream ostream, boolean onlyIfCached, String hostName) {
        //Client's ostream
        DataOutputStream out = new DataOutputStream(ostream);
        byte buffer[]  = new byte[BUF_SIZE]; 
        int count; 

         try {
            while ( (count = istream.read( buffer, 0, BUF_SIZE)) > -1)  {
                  out.write(buffer, 0, count); 
             }
            out.flush();
            out.close();
            istream.close();
        } catch(SocketTimeoutException e) {
            try {
                out.close();
                istream.close();
                if (onlyIfCached) {
                    if (cache.containsKey(hostName)) {  
                        writeResponseFromCache(cache.get(hostName).getFilePath(), ostream, hostName);
                    } else {
                        ostream.write(getErrorMessage("HTTP/1.1 504 Gateway Timeout\r\n"));
                        ostream.flush();
                        ostream.close();
                    }
                } else {
                    ostream.write(getErrorMessage("HTTP/1.1 504 Gateway Timeout\r\n"));
                    ostream.flush();
                    ostream.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            
        }catch (IOException e) {
            e.printStackTrace();
        }
         
    }
    
    /**
     * 
     * This will display a blocked site.
     * 
     * @param uri
     * @return
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
     * a dissallowed MIME type.
     * 
     * @return
     */
    public byte[] getErrorMessage() {
        String data = "<html><head><meta meta http-equiv=\"content-type\" content=\"text/html; "
                + "charset=ISO-8859-1\"></head><body>Forbidden Content!</body></html>";
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
     * This will display the error from an 
     * HttpURLConnection requestLine.
     * 
     * @param responseMessage
     * @return
     */
    private byte[] getErrorMessage(String responseMessage) {
        String data = "<html><head><meta meta http-equiv=\"content-type\" content=\"text/html; "
                + "charset=ISO-8859-1\"></head><body>Error Occured!</body></html>";
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