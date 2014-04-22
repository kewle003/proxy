package proxy;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class ProxyThread extends Thread {
    
    protected Socket clientSocket;
    protected Socket serverSocket;
    protected ConfigFile configFile;
    protected OutputStream rawOut;
    protected InputStream rawIn;
    
    private ByteArrayOutputStream data;
    
    private int BUF_SIZE = 8192;
    
   // protected String config;
    private int threadNum;
    
    private static int threadCount = 0;
    
    public ProxyThread(Socket clientSocket) {
        super("ProxyThread");
        this.clientSocket = clientSocket;
        this.threadNum = threadCount;
        threadCount++;
    }
    
    
    /* This method is used to handle client requests */ 
    public synchronized void run() {         
        //System.out.println("Thread "+ threadNum + " read at "+ clientSocket.getInetAddress());
        // read the request from the client 
        try {
            if (!clientSocket.isClosed()) {
                //Get the input stream from the client
                InputStream istream = clientSocket.getInputStream();
                OutputStream ostream = clientSocket.getOutputStream();
            
                HTTPRequest httpReq = new HTTPRequest();
                httpReq.parseRequest(istream);
            
                if (httpReq.getHost().length() != 0) {
                    serverSocket =  new Socket(httpReq.getHost(), httpReq.getPort());
                    rawOut = serverSocket.getOutputStream();
                    rawIn = serverSocket.getInputStream();
                } else {
                    return;
                }
            
               // System.out.println("****DEBUG**** Server connected to Port:" +serverSocket.getPort()+ " InetAddr: " +serverSocket.getInetAddress());
            
                // check if the request is for one of the disallowed domains. 
                // If the request is for a disallowed domain then inform the 
                // client in a HTTP response. 
                if (!httpReq.isAllowed(ProxyServer.getConfigFile())) {
                    ostream.write(getBlockedSiteScreen(httpReq.getRequestLine().getURI()));
                    clientSocket.close();
                    serverSocket.close();
                    return;
                }
                // satisfy the request from the local cache if possible 
                // if the request cannot be satisfied from the local cache 
                // then form a valid HTTP request and send it to the server. 
                if (ProxyServer.getCache().containsKey(httpReq.getHost())) {
                    System.out.println("*******************WRITING CACHE DATA**************");
                    System.out.println("FOR: " +httpReq.getHost());
                    //System.out.println(ProxyServer.getCache().get(httpReq.getHost()).getData()
                    ostream.write(ProxyServer.getCache().get(httpReq.getHost()).getData());
                    clientSocket.close();
                    serverSocket.close();
                    return;
                }
                String terminator = new String("Connection:close\n\n"); /* Prof said we should do this for this assignment */
                rawOut.write(httpReq.getRequestData());
                rawOut.write(terminator.getBytes());
                // check the Content-Type: header field of the response. 
                // if the type is not allowed then inform the client in a 
                // HTTP response. 
                HTTPResponse httpResp = new HTTPResponse();
                httpResp.parseHeaders(httpReq.getRequestLine().getURI());
            
           
                // Send the response to the client
                if (httpReq.getDissAllowedArgs().size() > 0) {
                    if (validateResponse(httpResp.getValueOfRequestHeader("Content-Type"), httpReq.getDissAllowedArgs()))  {
                        InputStream temp = rawIn;
                        writeResponse(temp, ostream);
                    } else {
                       // System.out.println("Should write error message");
                        ostream.write(getErrorMessage());
                        serverSocket.close();
                        clientSocket.close();
                        return;
                    }
                } else {
                    writeResponse(rawIn, ostream);
                }
           
                //System.out.println ("Client exit.");
                //System.out.println ("---------------------------------------------------");
           
                serverSocket.close();
                clientSocket.close();
            
                // Cache the content locally for future use 
                if (httpResp.isCacheable()) {
                    //System.out.println("----------------CACHING ALLOWED------------");
                    //Cache stuff
                    if (ProxyServer.getCache().get(httpReq.getHost()).isExpired()) {
                        System.out.println("Max-age=" + httpResp.getMaxAge());
                        System.out.println("Max-stale=" +httpResp.getMaxStale());
                        Cache c = new Cache((System.currentTimeMillis() + httpResp.getMaxAge()), (System.currentTimeMillis() + httpResp.getMaxStale()));
                        if (data != null) {
                            c.writeData(data.toString().getBytes());
                            ProxyServer.getCache().put(httpReq.getHost(), c);
                        }
                    }
                } else {
                   // System.out.println("----------------CACHING NOT ALLOWED---------");
                }
            }  
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (!clientSocket.isClosed())
                    clientSocket.close();
                if (serverSocket != null) {
                    if (!serverSocket.isClosed()) {
                        serverSocket.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        data = new ByteArrayOutputStream(BUF_SIZE);
        PrintWriter dataWriter = new PrintWriter(data);
        DataOutputStream out = new DataOutputStream(ostream);
        byte buffer[]  = new byte[BUF_SIZE]; 
        int count; 
         try {
            while ( (count = istream.read( buffer, 0, BUF_SIZE)) > -1)  {
                  out.write(buffer, 0, count); 
                  dataWriter.print(buffer);
             }
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataWriter.flush();
    }
    
    
    
    /**
     * 
     * This method will check whether or not the MIME types are allowed
     * in the Content-Type header from the HTTP response.
     * 
     * @param args
     * @param dissAllowedArgs
     * @return
     */
    public boolean validateResponse(List<String> args, List<String> dissAllowedArgs) {
        String allImages = "image/*";
        boolean blockAllImages = false;
        if (dissAllowedArgs.contains(allImages)) {
            blockAllImages = true;
        }
        if (args.size() > 0) {
            for (String arg : args) {
                if (blockAllImages) {
                    if (arg.contains("image")) {
                        //System.out.println("************Disallowed Type: " +arg);
                        return false;
                    }
                } else if (dissAllowedArgs.contains(arg)) {
                    //System.out.println("**************Disallowed Type: " +arg);
                    return false;
                }
            }
        }
       // System.out.println("################ALLOWED: " +args.toString());
        return true;
    }
    
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
    
    
}
