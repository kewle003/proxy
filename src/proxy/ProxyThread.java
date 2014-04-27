package proxyTest;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyThread extends Thread {
    
    protected Socket clientSocket;
    protected Socket serverSocket;
    protected Socket cacheServerSocket;
    protected ConfigFile configFile;
    protected OutputStream rawOut;
    protected InputStream rawIn;
    

    private int BUF_SIZE = 8192;
   
    public static int THREAD_COUNT = 0;
    
    private String host;
    
    
    

    public ProxyThread(Socket clientSocket) {
        super("ProxyThread");
        this.clientSocket = clientSocket;
        THREAD_COUNT++;

    }
    
    /**
     * 
     * This method will handle requests
     * from a client.
     * 
     */
    public synchronized void run() {  
       if (clientSocket.isConnected()) {
           try {
               HTTPRequest httpReq = new HTTPRequest(clientSocket);

               if (httpReq.getURI() == null) {
                   return;
               }
               
               httpReq.setDissAllowedMIME(ProxyServer.getConfigFile().getDissallowedDomains().get(httpReq.getHost()));
               if (httpReq.getDissAllowedMIME().contains("all")) {
                   writeResponse(getBlockedSiteScreen(httpReq.getURI()), httpReq.getClientSocket().getOutputStream());
                   httpReq.getClientSocket().close();
               } else {
               
                   HTTPResponse httpResp = new HTTPResponse();
                   httpResp.parseResponse(httpReq);

                   if (httpResp.isImage()) {
                       if (httpResp.isInValidContent()) {
                           writeResponse(getErrorMessage(), httpReq.getClientSocket().getOutputStream());
                       } else {
                           writeResponse(httpResp.getHttpUrlConnection().getInputStream(), httpReq.getClientSocket().getOutputStream());
                       }
                   } else {
                       writeResponse(httpResp.getData().getBytes(), httpReq.getClientSocket().getOutputStream());
                   }
                   httpReq.getClientSocket().close();
               }

           } catch (IOException e) {
               e.printStackTrace();
               System.out.println(host);
           } 
       }
       
    } 
    
    
    /**
     * 
     * This method writes data from the server to the
     * client.
     * 
     * @param bytes
     * @param ostream
     */
    private void writeResponse(byte[] bytes, OutputStream ostream) {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        DataOutputStream out = new DataOutputStream(ostream);
       // BufferedReader buf  = new BufferedReader(new InputStreamReader(in));
        int count;
        byte[] buffer = new byte[BUF_SIZE];
        try {
            
            while ((count = in.read(buffer, 0, BUF_SIZE)) > -1) {
                out.write(buffer, 0, count);
            }
            out.flush();
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 
     * This method simply writes the bytes from the
     * Server's socket to the Client's socket.
     * 
     * @param istream
     * @param ostream
     */
    public synchronized void writeResponse(InputStream istream, OutputStream ostream) {
        //Client's ostream
        DataOutputStream out = new DataOutputStream(ostream);
        byte buffer[]  = new byte[BUF_SIZE]; 
        int count; 

         try {
            while ( (count = istream.read( buffer, 0, BUF_SIZE)) > -1)  {
                  out.write(buffer, 0, count); 
             }
            out.flush();
            out.close();
            //dataWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
         
    }


    /**
     * 
     * This method will replace all
     * images of an html file if they are disallowed
     * by an extension.
     * 
     * @param line
     * @param extension
     * @return
     */
    private String validateLine(String line, List<String> extensionList) {
        System.out.println(extensionList.toString());
        Pattern srcReg = Pattern.compile("src\\s*=\\s*");
        if (extensionList.contains("star")) {
            //Block all images
            System.out.println("-----BLOCKING ALL------");
            Matcher m = srcReg.matcher(line);
            if (m.find())
                line = line.replaceAll("src\\s*=\\s*\".*\"", "src = \"\"");
            return line;
        } else if (extensionList.contains("none")) {
            return line;
        } else {
            for (String extension : extensionList) {
                System.out.println("CHECKING: " +extension);
                if (line.contains("." + extension.toLowerCase())) {
                    System.out.println("----------BLOCKING " +extension.toLowerCase()+ "-----------");
                    Matcher m = srcReg.matcher(line);
                    if (m.find())
                        line = line.replaceAll("src\\s*=\\s*\".*\\."+extension.toLowerCase()+"\"", "src = \"\"");
                    return line;
                }
            }
        }
        
        return line;
        
    }
    
    /**
     * 
     * This will display a blocked site.
     * 
     * @param uri
     * @return
     */
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
    
    /**
     * 
     * This may be used to block sites that contain
     * a dissallowed MIME type.
     * 
     * @return
     */
    public byte[] getErrorMessage() {
        String data = "<html><head><meta meta http-equiv=\"content-type\" content=\"text/html; "
                + "charset=ISO-8859-1\"></head><body>Forbidden Content!</body></html>";
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