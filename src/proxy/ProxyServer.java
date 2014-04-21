package proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ProxyServer {
    
    private static String CWD = System.getProperty("user.dir");
    private static String OS = System.getProperty("os.name").toLowerCase();
    private static Logger logger  = Logger.getLogger("MyLogger");
    
    // Data Members: 
    int proxyPort; // Proxy�s port
    ServerSocket proxySock; // The Proxy Server will listen on this socket 
    
    // serverSock represents the Proxy�s connection with a HTTP server 
    Socket serverSock; 
    
    HashMap<String, Cache> cache = new HashMap<String, Cache>();
    
    // Constructor 
    public ProxyServer (String configfilePath, int portNo) {

        FileHandler fileHandler;

        try {
            // This block configure the logger with handler and formatter
            fileHandler = new FileHandler("LoggerFile");
            logger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);

            // the following statement is used to log any messages
            logger.info("yay!!!");

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ConfigFile cf = new ConfigFile(configfilePath);
        proxyPort = portNo;

        try {
            proxySock = new ServerSocket(proxyPort);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Server UP");
        System.out.println("Server Up");
        System.out.println("Server Details: " + proxySock.toString());
        while (true) { 
            try {
                new ProxyThread(proxySock.accept(), cf, cache).start();
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

        if (OS.indexOf("win") >= 0) {
            new ProxyServer(CWD + "\\src\\proxy\\config.txt", portNo);
        } else {
            new ProxyServer(CWD + "/src/proxy/config.txt", portNo);
        } 
    } 

}
