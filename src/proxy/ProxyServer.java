package proxy;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ProxyServer {
    
    private static String CWD = System.getProperty("user.dir");
    private static String OS = System.getProperty("os.name").toLowerCase();
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
    
    /**
     * 
     * Default Constructor
     * 
     * @param configfilePath - The path to our config file
     * @param portNo - the port that our proxy server runs on
     * @param proxyCacheDir - the ProxyServerCache directory
     */
    public ProxyServer (String configfilePath, int portNo) {

        //Add the thread that waits for the program to be killed to delete ProxyServerCache
      //Create our ProxyServerCache directory
        final File proxyCacheDir = new File(CWD + "/ProxyServerCache");
        if (!proxyCacheDir.exists()) {
            logger.info("Creating Directory: " +proxyCacheDir.toString());
            proxyCacheDir.mkdir();
        } else {
            logger.info("Directory exists whyyyyyy");
        }
        
        FileHandler fileHandler;
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
            configFile =  new ConfigFile(configfilePath);
            // This block configure the logger with handler and formatter
            fileHandler = new FileHandler("LoggerFile");
            logger.addHandler(fileHandler);
            
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);

            // the following statement is used to log any messages
            logger.config("yay!!!");

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        proxyPort = portNo;

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
                new ProxyThread(proxySock.accept(), logger).start();
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
     * @return
     */
    protected static ConfigFile getConfigFile() {
        return configFile;
    }
    
    // main method 
    public static void main(String args[]) throws IOException { 
        // Read the config file name and the Proxy Port. 
        // Do error checking. If no config file is specified, or if no Port is specified 
        // then exit. 
        if (args.length != 1) {
            throw new IOException("Usage: ProxyServer [0-9999]");
        }

        int portNo = Integer.parseInt(args[0]);
        
        if (OS.indexOf("win") >= 0) {
            new ProxyServer(CWD + "\\src\\proxy\\config.txt", portNo);
        } else {
            new ProxyServer(CWD + "/src/proxy/config.txt", portNo);
        } 
        
        
    } 
    
    /**
     * 
     * This method will recurse through
     * the ProxyServerCache directory and delete
     * all files. The trick is is that security blocks
     * us from deleting directories if there is other
     * files within the directory.
     * 
     * @param file
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
