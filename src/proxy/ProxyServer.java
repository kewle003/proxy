package proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyServer {
    
    private static String CWD = System.getProperty("user.dir") + "/src/proxy/";
    // Data Members: 
    int proxyPort; // Proxy’s port 
    ServerSocket proxySock; // The Proxy Server will listen on this socket 
    
    // serverSock represents the Proxy’s connection with a HTTP server 
    Socket serverSock; 
    
    // Constructor 
    public ProxyServer (String configfilePath, int portNo) { 
        // Read the config file, and populate appropriate data structures. 
        // Create Server socket on the proxyPort and wait for the clients to 
        // connect. 
        ConfigFile cf = new ConfigFile(configfilePath);
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
                new ProxyThread(proxySock.accept(), cf).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } // end of while 
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
        
        System.out.println("Working Directory = " +
                CWD);
        
        int portNo = Integer.parseInt(args[0]);
        
        new ProxyServer(CWD + "config.txt", portNo);
    } 

}
