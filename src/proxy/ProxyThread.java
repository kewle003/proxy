package proxy;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyThread extends Thread {
    
    protected Socket clientSocket;
    protected Socket serverSocket;
    protected Socket cacheServerSocket;
    protected ConfigFile configFile;
    protected OutputStream rawOut;
    protected InputStream rawIn;
    private static HashMap<String, Cache> cache = new HashMap<String, Cache>();
    private Logger logger;
    

    private int BUF_SIZE = 8192;
   
    public static int THREAD_COUNT = 0;
    
    private String host;
    
    
    

    public ProxyThread(Socket clientSocket, Logger logger) {
        super("ProxyThread");
        this.clientSocket = clientSocket;
        this.logger = logger;
        THREAD_COUNT++;

    }
    
    /**
     * 
     * This method will handle requests
     * from a client.
     * 
     */
    public synchronized void run() {  
       if (clientSocket.isConnected()) {
           try {
               HTTPRequest httpReq = new HTTPRequest(clientSocket);
              // System.out.println(cache.keySet());
               if (httpReq.getURI() == null) {
                   return;
               }
               
               httpReq.setDissAllowedMIME(ProxyServer.getConfigFile().getDissallowedDomains().get(httpReq.getHost()));
               if (httpReq.getDissAllowedMIME().contains("all")) {
                   writeResponse(getBlockedSiteScreen(httpReq.getURI()), httpReq.getClientSocket().getOutputStream());
                   httpReq.getClientSocket().close();
               } else {
               
                   HTTPResponse httpResp = new HTTPResponse();
                   httpResp.parseResponse(httpReq);
                   if (httpResp.getHttpUrlConnection().getResponseCode() > 400) {
                       writeResponse(getErrorMessage(httpResp.getHttpUrlConnection().getResponseMessage()), httpReq.getClientSocket().getOutputStream());
                       httpReq.getClientSocket().close();
                       return;
                   }
                   httpResp.getHttpUrlConnection().connect();
                   if (httpResp.isImage()) {
                       if (httpResp.isInValidContent()) {
                           writeResponse(getErrorMessage(), httpReq.getClientSocket().getOutputStream());
                       } else {
                           writeResponse(httpResp.getHttpUrlConnection().getInputStream(), httpReq.getClientSocket().getOutputStream());
                       }
                   } else {
                       
                       
                       if (httpResp.isCacheAble()) {
                           if (cache.containsKey(httpReq.getHost())) {
                               Cache cacheToCheck = cache.get(httpReq.getHost());
                               if (cacheToCheck.isExpired()) {
                                   //logger.info(""+httpReq.getHost()+"::expired");
                                   System.out.println("Expired for: " +httpReq.getHost());
                                   Cache newCache = new Cache(httpResp.getMaxAgeCache(), httpResp.getMaxStaleCache(), httpReq.getHost());
                                   newCache.writeData(httpResp.getData(), logger);
                                   cache.put(httpReq.getHost(), newCache);
                                   writeResponseFromCache(newCache.getFilePath(), httpReq.getClientSocket().getOutputStream(), httpReq.getHost());
                               } else {
                                   System.out.println("Cache not expired for: " +httpReq.getHost());
                                   writeResponseFromCache(cacheToCheck.getFilePath(), httpReq.getClientSocket().getOutputStream(), httpReq.getHost());
                               }
                           } else {
                               System.out.println("Cache doesnt not exist: " +httpReq.getHost());
                               Cache newCache = new Cache(httpResp.getMaxAgeCache(), httpResp.getMaxStaleCache(), httpReq.getHost());
                               newCache.writeData(httpResp.getData(), logger);
                               cache.put(httpReq.getHost(), newCache);
                               writeResponseFromCache(newCache.getFilePath(), httpReq.getClientSocket().getOutputStream(), httpReq.getHost()); 
                           }
                       } else {
                           //logger.info(httpReq.getHost()+"::contacted orgin server");
                           writeResponse(httpResp.getData().getBytes(), httpReq.getClientSocket().getOutputStream());
                       }
                   }
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
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
     */
    private void writeResponse(byte[] bytes, OutputStream ostream) {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        DataOutputStream out = new DataOutputStream(ostream);
       // BufferedReader buf  = new BufferedReader(new InputStreamReader(in));
        int count;
        byte[] buffer = new byte[BUF_SIZE];
        try {
            
            while ((count = in.read(buffer, 0, BUF_SIZE)) > -1) {
                out.write(buffer, 0, count);
            }
            out.flush();
            in.close();
            out.close();
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
            out.close();
            //dataWriter.flush();
        } catch (IOException e) {
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
    
    private byte[] getErrorMessage(String responseMessage) {
        String data = "<html><head><meta meta http-equiv=\"content-type\" content=\"text/html; "
                + "charset=ISO-8859-1\"></head><body>Error Occured!</body></html>";
        StringBuilder errScreen = new StringBuilder("");
        errScreen.append(responseMessage);
        errScreen.append("Content-Type: text/html\r\n");
        errScreen.append("Content-Length: ");
        errScreen.append(data.length());
        errScreen.append("\r\n\r\n");
        errScreen.append(data);
        return errScreen.toString().getBytes();
    }
    
    
}