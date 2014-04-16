package proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyServer {
    
    // Data Members: 
    int proxyPort; // Proxy’s port 
    ServerSocket proxySock; // The Proxy Server will listen on this socket 
    
    // clientSock represents the Proxy’s connection with the Client 
    Socket clientSock; ;
    
    // serverSock represents the Proxy’s connection with a HTTP server 
    Socket serverSock; 
    
    // Constructor 
    public ProxyServer (String configfilePath) { 
        // Read the config file, and populate appropriate data structures. 
        // Create Server socket on the proxyPort and wait for the clients to 
        // connect. 
    
        try {
            proxySock = new ServerSocket(proxyPort);
        } catch (IOException e) {
            e.printStackTrace();
        } 
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
        // read the request from the client 
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
    public static void main(String args[]) { 
        // Read the config file name and the Proxy Port. 
        // Do error checking. If no config file is specified, or if no Port is specified 
        // then exit. 
    } 

}
