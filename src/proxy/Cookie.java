package proxy;

import java.util.StringTokenizer;

public class Cookie {
    
    private long checkAge;
    
    private StringBuilder value;
    
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
    }
    
    private void parseMaxAge(String val) {
        StringTokenizer st = new StringTokenizer(val, "=");
        st.nextToken(); //Ignore Max-age
        checkAge = (Integer.parseInt(st.nextToken())*1000 + System.currentTimeMillis());
    }

    public boolean cookieExpired() {
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
