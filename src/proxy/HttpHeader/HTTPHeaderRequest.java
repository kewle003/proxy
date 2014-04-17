package proxy.HttpHeader;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * 
 * 
 * Request = Request-Line
 *          *(( general-header       ; Section 4.5
 *           | request-header        ; Section 5.3
 *           | entity-header) CRLF)  ; Section 7.1
 *           CRLF
 *           [ message-body ]        ; Section 4.3
 * 
 * Request-Line = Method SP Request-URI SP HTTP-Version CRLF
 * @author mark
 *
 */
public class HTTPHeaderRequest extends HTTPHeader {
    
    /**
     * The Method token indicates the method to be performed on the resource
     * identified by the Request-URI. The method is case-sensitive.
     * @author mark
     *
     */
    private enum HttpMethod {
        OPTIONS,
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
        TRACE,
        CONNECT,
        extension_method
    }
    
    /**
     * The request-header fields allow the client to pass additional
     * information about the request, and about the client itself,
     * to the server. These fields act as request modifiers, with
     * semantics equivalent to the parameters on a programming 
     * language method invocation.
     * 
     * request-header = RequestHeader
     * 
     * @author mark
     *
     */
    private enum RequestHeader {
        Accept,
        Accept_Charset,
        Accept_Encoding,
        Accept_Language,
        Authorization,
        Except,
        From,
        Host,
        If_Match,
        If_Modified_Since,
        If_None_Match,
        If_Range,
        If_Unmodified_Since,
        Max_Forwards,
        Proxy_Authorization,
        Range,
        Referer,
        TE,
        User_Agent
    }
    
    public RequestHeader reqHeaderVal;
    public String headerVal;
    
    public HTTPHeaderRequest() {
        
    }

    @Override
    public boolean parseHeader(String stringToParse) {
        StringTokenizer st = new StringTokenizer(stringToParse);
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String toString() {
        String retVal = reqHeaderVal.toString();
        retVal = retVal.replace("_", "-");
        retVal = retVal.concat("=");
        retVal.concat(headerVal);
        return retVal;
    }

}
