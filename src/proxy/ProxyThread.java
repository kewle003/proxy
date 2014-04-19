package proxy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

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
        StringTokenizer st;
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
                System.out.println("Disallowed domain encounterd: " +httpReq.getHost());
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
            
            /**
             * This is the only way I know that we can get headers from response without
             * affecting CHUNKED writes
             */
            URL obj = new URL(httpReq.getRequestLine().getURI());
            URLConnection conn = obj.openConnection();
         
            //get all headers
            System.out.println("******** Recieved Headers for debugging  **************");
            Map<String, List<String>> headerFields = conn.getHeaderFields();
    
            Set<String> headerFieldsSet = headerFields.keySet();
            Iterator<String> hearerFieldsIter = headerFieldsSet.iterator();

            while (hearerFieldsIter.hasNext()) {
                String headerFieldKey = hearerFieldsIter.next();
                List<String> headerFieldValue = headerFields.get(headerFieldKey);
                         
                StringBuilder sb = new StringBuilder();
     
                for (String value : headerFieldValue) {
                    sb.append(value);
                    sb.append("");
                }
                System.out.println(headerFieldKey + "=" + sb.toString());
                if (headerFieldKey != null) {
                    if (headerFieldKey.equals("Transfer-Encoding")) {
                        if (sb != null) {
                            if (sb.toString().toLowerCase().equals("chunked")) {
                                System.out.println("##################CHUNKKKKKEEEDDDDD FUUUUCKKKKK####################");
                                isChunked = true;
                            }
                        }
                    }
                }
            }
            System.out.println("******** End of Recieved Headers for debugging  **************");
          
            // Send the response to the client
            writeResponse(rawIn, ostream);
           
            System.out.println ("Client exit.");
            System.out.println ("---------------------------------------------------");
           
            serverSocket.close();
            clientSocket.close();
            
            // Cache the content locally for future use 
            
            
        } catch (IOException e) {
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
}
