package proxy;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.StringTokenizer;

public class ProxyServer {
    
    // Data Members: 
    int proxyPort; // ProxyÕs port 
    ServerSocket proxySock; // The Proxy Server will listen on this socket 
    
    // clientSock represents the ProxyÕs connection with the Client 
    Socket clientSock; ;
    
    // serverSock represents the ProxyÕs connection with a HTTP server 
    Socket serverSock; 
    
    // Constructor 
    public ProxyServer (String configfilePath, int portNo) { 
        // Read the config file, and populate appropriate data structures. 
        // Create Server socket on the proxyPort and wait for the clients to 
        // connect. 
        //ConfigFile cf = new ConfigFile(configfilePath);
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
        URL url;
        HttpURLConnection connection = null;
        System.out.println("Client read at "+ clientSocket.getInetAddress());
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
              response.append(line);
              response.append('\r');
            }
            rd.close();
            //System.out.println(response.toString());
            // check the Content-Type: header field of the response. 
            // if the type is not allowed then inform the client in a 
            // HTTP response. 
            // Send the response to the client 
            // Cache the content locally for future use 
            
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       
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
