package proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class ProxyServer {
    
    private static String CWD = System.getProperty("user.dir");
    private static String OS = System.getProperty("os.name").toLowerCase();
    
    // Data Members: 
    int proxyPort; // ProxyÕs port 
    ServerSocket proxySock; // The Proxy Server will listen on this socket 
    
    // serverSock represents the ProxyÕs connection with a HTTP server 
    Socket serverSock; 
    
    // Constructor 
    public ProxyServer (String configfilePath, int portNo) { 
        // Read the config file, and populate appropriate data structures. 
        // Create Server socket on the proxyPort and wait for the clients to 
        // connect. 
        //Set up the CacheManager
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
        System.out.println("OS detected: " +OS);
        /**
         * This is how we will implement the cache max-age
         * what we will do is grab the currentTime using Date.
         * We will compare that to the cached date that we created.
         */
        Date dt = new Date(2014, 04, 16, 22, 42);
        Date madeUp = new Date(2014, 04, 16, 22, 42);
        System.out.println(dt.toGMTString());
        System.out.println(madeUp.toGMTString());
        if (dt.equals(madeUp)) {
            System.out.println("Dates are equal!");
        } else {
            System.out.println("Dates are not equal...WHYYYY");
        }
        if (OS.indexOf("win") >= 0) {
            new ProxyServer(CWD + "\\src\\proxy\\config.txt", portNo);
        } else {
            new ProxyServer(CWD + "/src/proxy/config.txt", portNo);
        } 
    } 

}
