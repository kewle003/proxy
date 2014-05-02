package proxy;


import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Logger;

/**
 * 
 * This is our main Engine to our Proxy Server.
 * One needs to instantiate this in their main Method
 * in order to use the Server. One must provide a configuration
 * file that is located in the same directory as ProxyServer.java.
 * 
 * @author mark
 *
 */
public class ProxyServerEngine extends Thread {
    
    private static String CWD = System.getProperty("user.dir");
    private static Logger logger  = Logger.getLogger("MyLogger");
    
    
    // Data Members: 
    /**
     * Proxy port for the server
     */
    int proxyPort;
    /**
     * The Proxy Server will listen on this socket 
     */
    ServerSocket proxySock; 
            
    
    /**
     * A handle on a ConfigFile object
     */
    private static ConfigFile configFile;
    
    private String configFilePath;
    
    public ProxyServerEngine(String configFilePath, int port) {
        this.configFilePath = configFilePath;
        proxyPort = port;
    }
    
    /**
     * 
     * Our thread run function. This will set up the ProxyServer
     * socket as well as accept all subsequent client request Sockets.
     * This is a multi-threaded server.
     * 
     */
    public synchronized void run () {

        //Add the thread that waits for the program to be killed to delete ProxyServerCache
      //Create our ProxyServerCache directory
        final File proxyCacheDir = new File(CWD+"/ProxyServerCache");
        if (!proxyCacheDir.exists()) {
            logger.info("Creating Directory: " +proxyCacheDir.toString());
            proxyCacheDir.mkdir();
        } else {
            logger.info("Directory exists whyyyyyy");
        }
        

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            //If shutdownHook called, delete ProxyServerCache
            public void run() {
                try {
                    deleteProxyCacheDirectory(proxyCacheDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));

        try {
            //Out configuration file
            configFile =  new ConfigFile(configFilePath);

            // the following statement is used to log any messages
            logger.config("yay!!!");

        } catch (SecurityException e) {
            e.printStackTrace();
        }
        //proxyPort = portNo;

        try {
            //Bind the proxyServer socket
            proxySock = new ServerSocket(proxyPort);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.config("Server UP");
        while (true) { 
            try {
                //Create a client socket thread
                new ProxyThread(proxySock.accept()).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } // end of while 
    } 
    
    
    /**
     * 
     * Static method used to retrieve the configFile.
     * I know this is bad programming practice.
     * 
     * @return ConfigFile - The handle on our ConfigFile
     */
    protected static ConfigFile getConfigFile() {
        return configFile;
    }
    
    /**
     * 
     * This method will recurse through
     * the ProxyServerCache directory and delete
     * all files. The trick is is that security blocks
     * us from deleting directories if there is other
     * files within the directory.
     * 
     * @param file - The directory to delete
     * @throws IOException
     * @author mkyong - www.mykong.com
     */
    public static void deleteProxyCacheDirectory(File file) throws IOException {
            if (file.isDirectory()) {
     
                //directory is empty, then delete it
                if (file.list().length==0) {
     
                   file.delete();
                } else {
     
                   //list all the directory contents
                   String files[] = file.list();
                   
                   //Iterate through all subdirectories/files
                   for (String temp : files) {
                      //construct the file structure
                      File fileDelete = new File(file, temp);
 
                      //Recursive Call
                     deleteProxyCacheDirectory(fileDelete);
                   }
     
                   //check the directory again, if empty then delete it
                   if (file.list().length==0) {
                     file.delete();
                   }
                }
     
            } else {
                //if file, then delete it
                file.delete();
            }
    } 
}
