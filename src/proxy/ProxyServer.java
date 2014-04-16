package proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class ProxyServer {
    
    // Data Members: 
    int proxyPort; // Proxy’s port 
    ServerSocket proxySock; // The Proxy Server will listen on this socket 
    
    // clientSock represents the Proxy’s connection with the Client 
    Socket clientSock; ;
    
    // serverSock represents the Proxy’s connection with a HTTP server 
    Socket serverSock; 
    
    // Constructor 
    public ProxyServer (String configfilePath, int portNo) { 
        // Read the config file, and populate appropriate data structures. 
        // Create Server socket on the proxyPort and wait for the clients to 
        // connect. 
        proxyPort = portNo;
    
        try {
            proxySock = new ServerSocket(proxyPort);
        } catch (IOException e) {
            e.printStackTrace();
        } 
        System.out.println("Server Up");
        System.out.println("Server Details: " + proxySock.toString());
        while (true) { 
            try {
                clientSock = proxySock.accept();
            } catch (IOException e) {
                e.printStackTrace();
            } 
            handleRequest(clientSock); 
        } // end of while 
    } 
    
    /* This method is used to handle client requests */ 
    private synchronized void handleRequest(Socket clientSocket) { 
        StringTokenizer st;
        
        System.out.println("Client read at "+ clientSocket.getInetAddress());
        // read the request from the client 
        try {
            //Get the input stream from the client
            InputStream istream = clientSocket.getInputStream();
            BufferedReader inLine = new BufferedReader(new InputStreamReader(istream));
            
            String requestLine = inLine.readLine();
            st = new StringTokenizer(requestLine);
            
            String request = st.nextToken();
            String uri = st.nextToken();
            String protocol = st.nextToken();
            
            System.out.println("Request: " +request);
            System.out.println("URI: " +uri);
            System.out.println("Protocol: " +protocol);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
        // check if the request is for one of the disallowed domains. 
        // If the request is for a disallowed domain then inform the 
        // client in a HTTP response. 
        // satisfy the request from the local cache if possible 
        // if the request cannot be satisfied from the local cache 
        // then form a valid HTTP request and send it to the server. 
        // read the response from the server. 
        // check the Content-Type: header field of the response. 
        // if the type is not allowed then inform the client in a 
        // HTTP response. 
        // Send the response to the client 
        // Cache the content locally for future use 
    } 
    // main method 
    public static void main(String args[]) throws IOException { 
        // Read the config file name and the Proxy Port. 
        // Do error checking. If no config file is specified, or if no Port is specified 
        // then exit. 
        if (args.length != 1) {
            System.out.println(args[0]);
            throw new IOException("Usage: ProxyServer [0-9999]");
        }
        
        int portNo = Integer.parseInt(args[0]);
        
        ProxyServer prox = new ProxyServer("", portNo);
    } 

}
