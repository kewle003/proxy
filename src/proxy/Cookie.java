package proxy;

import java.util.List;
import java.util.StringTokenizer;

/**
 * 
 * This class handles storing a Cookie. It will
 * store the Set-Cookie header value from an HTTP
 * Response. Furthermore, it will store the age of the
 * cookie in milliseconds. If the Cookie has Expired it 
 * you should delete it.
 * 
 * @author mark
 *
 */
public class Cookie {
    
    //Age we use to check against currentTime
    private long checkAge;
    
    //The cookie data
    private StringBuilder value;
    
    //If maxAge was not specified
    private boolean noMaxAge = false;
    
    /**
     * 
     * Constructor for the Set-Cookie Response
     * header if there is only one cookie.
     * 
     * @param data
     */
    public Cookie(String data) {
        value = new StringBuilder("");
        StringTokenizer st = new StringTokenizer(data);
        while (st.hasMoreTokens()) {
            String val = st.nextToken();
            value.append(val);
            if (val.contains("Max-Age")) {
                parseMaxAge(val);
            }
        }
        
        if (!value.toString().contains("Max-Age")) {
            noMaxAge = true;
        }
    }
    
    /**
     * 
     * Constructor for the Set-Cookie Response
     * header if there is more than one cookie.
     * 
     * @param headerFieldValue - List<String> cookie data
     */
    public Cookie(List<String> headerFieldValue) {
        value = new StringBuilder("");
        for (String data : headerFieldValue) {
            StringTokenizer st = new StringTokenizer(data);
            value.append(st.nextToken());
        }
            
    }

    private void parseMaxAge(String val) {
        StringTokenizer st = new StringTokenizer(val, "=");
        st.nextToken(); //Ignore Max-age
        checkAge = (Integer.parseInt(st.nextToken())*1000 + System.currentTimeMillis());
    }
    
    /**
     * 
     * Checks if the cookie has expired.
     * 
     * @return true - has expired, false otherwise
     */
    public boolean cookieExpired() {
        if (noMaxAge)
            return false;
        long checker = System.currentTimeMillis();
        if (checker > checkAge) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * 
     * Default toString()
     * 
     */
    public String toString() {
        return value.toString();
    }

}
