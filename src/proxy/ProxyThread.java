package proxy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class ProxyThread extends Thread {
    
    protected Socket clientSocket;
    protected Socket serverSocket;
    protected ConfigFile configFile;
    protected OutputStream rawOut;
    protected InputStream rawIn;
    
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
        String host = "";
        int port = 0;
        
        System.out.println("Thread "+ threadNum + " read at "+ clientSocket.getInetAddress());
        // read the request from the client 
        try {
            //Get the input stream from the client
            InputStream istream = clientSocket.getInputStream();
            OutputStream ostream = clientSocket.getOutputStream();
            
            //Create a Line Buffer for reading a line at a time
            BufferedReader inLine = new BufferedReader(new InputStreamReader(istream));
            
            //Create output buffers for the serverSocket
            ByteArrayOutputStream headerBuf = new ByteArrayOutputStream(8096);
            PrintWriter headerWriter = new PrintWriter( headerBuf );
            
            
            //Read the request-line
            String requestLine = inLine.readLine();
            headerWriter.println(requestLine);
            
            st = new StringTokenizer(requestLine);
            String request = st.nextToken(); /* GET, POST, HEAD */
            String uri = st.nextToken(); /* http://www.example.com */
            String protocol = st.nextToken(); /* HTTP1.1/1.0 */
            
            if (!protocol.equals("HTTP/1.1")) {
                throw new IOException("Invalid HTTP protocol");
            }
            
            System.out.println("*****DEBUG**** request-line: Request=" +request+ " uri=" +uri+ " protocol=" +protocol);
            
            requestLine = inLine.readLine();
            System.out.println(requestLine);
            if (requestLine.contains("Host")) {
                st = new StringTokenizer(requestLine, ": ");
                String fieldName = st.nextToken(); /* Expect HOST fieldName */
                if (fieldName.equals("Host")) {
                    host = st.nextToken();
                    String portString = new String("");
                    try {
                        portString = st.nextToken();
                    } catch (Exception NoSuchElement) {
                        System.out.println("No port discovered");
                    }
                    //If there was no port, we will have an empty string
                    if (portString.length() == 0) {
                        port = 80;
                    } else {
                        port = Integer.parseInt(portString);
                    }
                }     
            }
            
            if (host.length() != 0) {
                serverSocket =  new Socket(host, port);
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
            if (domainHash.containsKey(host)) {
                System.out.println("Disallowed domain encounterd: " +host);
            } else {
                System.out.println("Domain allowed: " +host);
            }
            // satisfy the request from the local cache if possible 
            // if the request cannot be satisfied from the local cache 
            // then form a valid HTTP request and send it to the server. 
            
            /* Read in the rest of the request */
            while (requestLine.length() != 0) {
                headerWriter.println(requestLine);
                requestLine = inLine.readLine();     
            }
            headerWriter.flush();
            System.out.println("******** Buffered Headers for debugging  **************");
            System.out.print( headerBuf.toString() );
            System.out.println("******** End of Buffered Headers for debugging  **************");
            
            String terminator = new String("Connection:close\n\n"); /* Prof said we should do this for this assignment */
            
            rawOut.write(headerBuf.toString().getBytes());
            rawOut.write(terminator.getBytes());
            
            // check the Content-Type: header field of the response. 
            // if the type is not allowed then inform the client in a 
            // HTTP response. 
            
            /**
             * This is the only way I know that we can get headers from response without
             * affecting CHUNKED writes
             */
            URL obj = new URL(uri);
            URLConnection conn = obj.openConnection();
         
            //get all headers
            System.out.println("******** Recieved Headers for debugging  **************");
            Map<String, List<String>> map = conn.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                System.out.println("Key : " + entry.getKey() + 
                         " Value : " + entry.getValue());
            }
            System.out.println("******** End of Recieved Headers for debugging  **************");
            
            // Send the response to the client
            
            byte buffer[]  = new byte[8192]; 
            int count; 
             while ( (count = rawIn.read( buffer, 0, 8192)) > -1)  {
                  ostream.write(buffer, 0, count);  
                   /*****************************************************************************************************************/
                   /****  You will need to add code to read response line and headers as this code does for the request headers  ****/ 
                   /*****************************************************************************************************************/
             }
           
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
}
