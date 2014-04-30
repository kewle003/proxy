package proxy;

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
    
    private long checkAge;
    
    private StringBuilder value;
    
    private boolean noMaxAge = false;
    
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
    
    private void parseMaxAge(String val) {
        StringTokenizer st = new StringTokenizer(val, "=");
        st.nextToken(); //Ignore Max-age
        checkAge = (Integer.parseInt(st.nextToken())*1000 + System.currentTimeMillis());
    }

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
    
    public String toString() {
        return value.toString();
    }

}
