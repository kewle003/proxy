package proxy;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProxyThread extends Thread {
    
    protected Socket clientSocket;
    protected Socket serverSocket;
    protected ConfigFile configFile;
    protected OutputStream rawOut;
    protected InputStream rawIn;
    
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
    
    /**
     * TODO:
     * Once up and working, String config will be ConfigFile or something
     * @param clientSocket
     * @param configFile
     * @param cm
     */
    public ProxyThread(Socket clientSocket, ConfigFile configFile) {
        super("ProxyThread");
        this.clientSocket= clientSocket;
        this.configFile = configFile;
    }
    
    /* This method is used to handle client requests */ 
    public synchronized void run() { 
        HashMap<String, List<String>> domainHash;
        boolean isChunked = false;
        
        System.out.println("Thread "+ threadNum + " read at "+ clientSocket.getInetAddress());
        // read the request from the client 
        try {
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
            
            System.out.println("****DEBUG**** Server connected to Port:" +serverSocket.getPort()+ " InetAddr: " +serverSocket.getInetAddress());
            
            // check if the request is for one of the disallowed domains. 
            // If the request is for a disallowed domain then inform the 
            // client in a HTTP response. 
            domainHash = configFile.getDissallowedDomains();
            if (domainHash.containsKey(httpReq.getHost())) {
                ArrayList<String> argumentList = (ArrayList<String>) domainHash.get(httpReq.getHost());
                if (argumentList.size() == 0) {
                    System.out.println("Disallowed domain encounterd: " +httpReq.getHost());
                    ostream.write(getBlockedSiteScreen(httpReq.getHost()));
                    clientSocket.close();
                    serverSocket.close();
                    return;
                } else if (argumentList.get(0).equals("*")) {
                    ostream.write(getBlockedSiteScreen(httpReq.getHost()));
                    clientSocket.close();
                    serverSocket.close();
                    return;
                }
            } else {
                System.out.println("Domain allowed: " +httpReq.getHost());
            }
            // satisfy the request from the local cache if possible 
            // if the request cannot be satisfied from the local cache 
            // then form a valid HTTP request and send it to the server. 
            String terminator = new String("Connection:close\n\n"); /* Prof said we should do this for this assignment */
            rawOut.write(httpReq.getRequestData());
            rawOut.write(terminator.getBytes());
            // check the Content-Type: header field of the response. 
            // if the type is not allowed then inform the client in a 
            // HTTP response. 
            HTTPResponse resp = new HTTPResponse();
            resp.parseHeaders(httpReq.getRequestLine().getURI());
            
            
          
            // Send the response to the client
            writeResponse(rawIn, ostream);
           
            System.out.println ("Client exit.");
            System.out.println ("---------------------------------------------------");
           
            serverSocket.close();
            clientSocket.close();
            
            // Cache the content locally for future use 
            
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                if (serverSocket != null) {
                    if (serverSocket.isConnected()) {
                        serverSocket.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
       
    } 
    
    public synchronized void writeResponse(InputStream istream, OutputStream ostream) {
        //Client's ostream
        DataOutputStream out = new DataOutputStream(ostream);
        byte buffer[]  = new byte[BUF_SIZE]; 
        int count; 
         try {
            while ( (count = istream.read( buffer, 0, BUF_SIZE)) > -1)  {
                  out.write(buffer, 0, count);  
             }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    
    
}
