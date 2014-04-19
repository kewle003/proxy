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
 * This class will handle setting up an HTTPRequest
 * 
 * @author mark
 *
 */
public class HTTPRequest {
    
    private int BUF_SIZE = 8096;
    
    private int port;
    private String host;
    private ByteArrayOutputStream headerBuf;
    private RequestLine reqLine;
    private List<HTTPHeader> headerList;
    
    public HTTPRequest() {
        port = 0;
        host = new String("");
        headerList = new ArrayList<HTTPHeader>();
    }

    public void parseRequest(InputStream istream) throws IOException {
        StringTokenizer st;
        host = new String("");
        port = 80;
        String requestLine = new String("");
        
        //Create a Line Buffer for reading a line at a time
        BufferedReader inLine = new BufferedReader(new InputStreamReader(istream));
        
        //Create output buffers for the serverSocket
        headerBuf = new ByteArrayOutputStream(BUF_SIZE);
        PrintWriter headerWriter = new PrintWriter( headerBuf );
        
        
        //Read the request-line
        requestLine = inLine.readLine();
        
        reqLine = new RequestLine(requestLine);
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
        System.out.println("******** Buffered Headers for debugging  **************");
        for (HTTPHeader header : headerList) {
            System.out.print(header.getHeaderName()+ " = ");
            for (String args : header.getArguments()) {
                System.out.print( args + ",");
            }
            System.out.print("\n");
        }
        System.out.println("******** End of Buffered Headers for debugging  **************");
        //System.out.println("******** WHAT THEY SHOULD BE ***************");
        //System.out.println(headerBuf.toString());
        //System.out.println("********* END OF WHAT THEY SHOULD BE ******************");
    }
    
    public String getValueOfRequestHeader(String header) {
        return "";
    }
    
    public byte[] getRequestData() {
        return headerBuf.toString().getBytes();
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
