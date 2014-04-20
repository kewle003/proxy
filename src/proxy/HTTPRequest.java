package proxy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class will handle setting up an HTTPRequest.
 * 
 * @author mark
 *
 */
public class HTTPRequest {
    
    private int BUF_SIZE = 8096;
    
    private int port;
    private String host;
    private ByteArrayOutputStream headerBuf;
    private PrintWriter headerWriter;
    private RequestLine reqLine;
    private List<HTTPHeader> headerList;
    private List<String> dissAllowedArgs;
    
    public HTTPRequest() {
        port = 0;
        host = new String("");
        headerList = new ArrayList<HTTPHeader>();
        this.dissAllowedArgs = new ArrayList<String>();
    }

    /**
     * 
     * This method will take in an InputStream object and will
     * read from the pipe. It will begin by first extracting the
     * request-line. Then it will extract the host and port for
     * a server socket connection. Finally it will read the rest
     * of the input. This method will store all HTTPHeader's in a list
     * as well as store a buffer of the input from the InputStream.
     * 
     * @param istream
     * @throws IOException
     */
    public void parseRequest(InputStream istream) throws IOException {
        StringTokenizer st;
        host = new String("");
        port = 80;
        String requestLine = new String("");
        
        //Create a Line Buffer for reading a line at a time
        BufferedReader inLine = new BufferedReader(new InputStreamReader(istream));
        
        //Create output buffers for the serverSocket
        headerBuf = new ByteArrayOutputStream(BUF_SIZE);
        headerWriter = new PrintWriter( headerBuf );
        
        
        //Read the request-line
        requestLine = inLine.readLine();
        
        reqLine = new RequestLine(requestLine);
        if (!reqLine.getMethod().equals("HEAD")) {
            headerWriter.println(requestLine);
                
            requestLine = inLine.readLine();
            System.out.println(requestLine);
            if (requestLine.contains("Host")) {
                st = new StringTokenizer(requestLine, ": ");
                String fieldName = st.nextToken(); /* Expect HOST fieldName */
                if (fieldName.equals("Host")) {
                    host = st.nextToken();
                    String portString = new String("");
                    try {
                        portString = st.nextToken();
                    } catch (Exception NoSuchElement) {
                        System.out.println("No port discovered");
                    }
                    //If there was no port, we will have an empty string
                    if (portString.length() == 0) {
                        port = 80;
                    } else {
                        port = Integer.parseInt(portString);
                    }
                }     
            }
        
            /* Read in the rest of the request */
            while (requestLine.length() != 0) {
                HTTPHeader header = new HTTPHeader();
                header.parseHeader(requestLine);
                headerList.add(header);
                headerWriter.println(requestLine);
                requestLine = inLine.readLine();
                
            }
            headerWriter.flush();
            /*System.out.println("******** Buffered Headers for debugging  **************");
            for (HTTPHeader header : headerList) {
                System.out.println(header.toString());
            }
            System.out.println("/r/n/r/n");
            System.out.println("******** End of Buffered Headers for debugging  **************");
            System.out.println("******** WHAT THEY SHOULD BE ***************");
            System.out.println(headerBuf.toString());
            System.out.println("********* END OF WHAT THEY SHOULD BE ******************");*/
        } else {
            //DO HEAD REQUEST
        }
    }
    
    /**
     * 
     * This method will grab the value of the
     * HTTP header. 
     * 
     * @param header
     * @return - the argument list if found, else null
     */
    public List<String> getValueOfRequestHeader(String headerName) {
        for (HTTPHeader header : headerList) {
            if (header.getHeaderName().equals(headerName)) {
                return header.getArguments();
            }
        }
        
        return null;
    }
    
    /**
     * 
     * This method checks whether or not the HOST is an allowed
     * domain for our Proxy. Furthermore, if it is an allowed domain,
     * it builds the dissAllowed MIME types for that HOST.
     * 
     * @param configFile
     * @return
     */
    public boolean isAllowed(ConfigFile configFile) {
        HashMap<String, List<String>> domainHash = configFile.getDissallowedDomains();
        //String hostToCheck = host.substring(5); /* Start without www */
        if (domainHash.containsKey(host)) {
            dissAllowedArgs = (ArrayList<String>) domainHash.get(host);
            if (dissAllowedArgs.size() == 0) {
                System.out.println("Disallowed domain encounterd: " +host);
               
                return false;
            } else if (dissAllowedArgs.get(0).equals("*")) {
                return false;
            }
        } 
        System.out.println("Domain allowed: " +host);
        System.out.println("Arguments: " +dissAllowedArgs.toString());
        return true;
    }
    
    /**
     * 
     * This method grabs the data as an array
     * of type <code>byte</code>.
     * 
     * @return - The data to be written to the server
     */
    public byte[] getRequestData() {
        return headerBuf.toString().getBytes();
    }
    

    public List<String> getDissAllowedArgs() {
        return dissAllowedArgs;
    }
    
    
    public List<HTTPHeader> getHeaders() {
        return headerList;
    }
    
    public RequestLine getRequestLine() {
        return reqLine;
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
}
