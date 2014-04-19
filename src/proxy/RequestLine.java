package proxy;

import java.util.StringTokenizer;

public class RequestLine {
    private String method; /* GET, POST, HEAD */
    private String uri; /* http://www.example.com */
    private String protocol; /* HTTP 1.1/1.0 */
    
    public RequestLine (String requestLine) {
        StringTokenizer st = new StringTokenizer(requestLine);
        
        method = st.nextToken().toUpperCase();
        uri = st.nextToken();
        protocol = st.nextToken().toUpperCase();
        
        System.out.println("*****DEBUG**** request-line: Method=" +method+ " uri=" +uri+ " protocol=" +protocol);

    }
    
    public String getMethod() {
        return method;
    }
    
    public String getURI() {
        return uri;
    }
    
    public String getProtocol() {
        return protocol;
    }
}
