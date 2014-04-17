package proxy;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.StringTokenizer;

public class ProxyThread extends Thread {
    
    protected Socket clientSocket;
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
     * @param config
     */
    public ProxyThread(Socket clientSocket, String config) {
        super("ProxyThread");
        this.clientSocket= clientSocket;
    }
    
    /* This method is used to handle client requests */ 
    public synchronized void run() { 
        StringTokenizer st;
        URL url;
        HttpURLConnection connection = null;
        
        System.out.println("Thread "+ threadNum + " read at "+ clientSocket.getInetAddress());
        // read the request from the client 
        try {
            //Get the input stream from the client
            InputStream istream = clientSocket.getInputStream();
            BufferedReader inLine = new BufferedReader(new InputStreamReader(istream));
            OutputStream ostream = clientSocket.getOutputStream();
            
            String requestLine = inLine.readLine();
            st = new StringTokenizer(requestLine);
            
            //GET/POST?
            String request = st.nextToken();
            //http://www.example.com/
            String uri = st.nextToken();
            //HTTP/1.1?
            String protocol = st.nextToken();
            
            if (!protocol.equals("HTTP/1.1")) {
                throw new IOException("Invalid HTTP protocol");
            }
            
           // while ((requestLine = inLine.readLine()) != null)
             //   System.out.println(requestLine);
            
           // if (uri.startsWith("http")) {
                url = new URL(uri);
            //}
            
          
            
            // check if the request is for one of the disallowed domains. 
            // If the request is for a disallowed domain then inform the 
            // client in a HTTP response. 
            // satisfy the request from the local cache if possible 
            // if the request cannot be satisfied from the local cache 
            // then form a valid HTTP request and send it to the server. 
            
            connection = (HttpURLConnection) url.openConnection();
            System.out.println("Here");
            connection.setRequestMethod(request);
            connection.setRequestProperty("Content-Type", 
                        "application/x-www-form-urlencoded");
           // connection.setRequestProperty("Content-Length","");
            connection.setRequestProperty("Content-Language", "en-US");
                
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
                
            //Send request
            DataOutputStream wr = new DataOutputStream (
                         connection.getOutputStream ());
            // wr.writeBytes(""); //urlParamters eventually
            wr.flush();
            wr.close();
            // read the response from the server. 
            //Get Response  
            InputStream is;
            if (connection.getResponseCode() >= 400) {
                is = connection.getErrorStream();
            } else {
                is = connection.getInputStream();
            }
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer(); 
            while((line = rd.readLine()) != null) {
              //response.append(line);
              //response.append('\r');
              ostream.write(line.toString().getBytes(Charset.forName("UTF-8")));
            }
            rd.close();
            //System.out.println(response.toString());
      
            //byte buffer[] = new byte[1024];
            
            //ostream.write(response.toString().getBytes(Charset.forName("UTF-8")));
            // check the Content-Type: header field of the response. 
            // if the type is not allowed then inform the client in a 
            // HTTP response. 
            // Send the response to the client 
            // Cache the content locally for future use 
            
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
       
    } 
}
