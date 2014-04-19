package proxy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 * 
 * This class represents any HTTP header as an object.
 * It contains the header name as well as it's argument
 * list stored as <code>List<String></code>.
 * 
 * @author mark
 *
 */
public class HTTPHeader {
    
    private String headerName;
    private List<String> arguments;
    private Date cacheAge;
    
    /**
     * Default Constructor
     */
    public HTTPHeader() {
        headerName = new String("");
        arguments = new ArrayList<String>();
    }
    
    /**
     * 
     * Used for dummies.
     * 
     * @param headerName
     */
    public HTTPHeader(String headerName) {
        this.headerName = headerName;
        arguments = new ArrayList<String>();
    }
    
    public HTTPHeader(String headerName, List<String> arguments) {
        this.headerName = headerName;
        this.arguments = arguments;
    }
    
    /**
     * 
     * This method will parse the header line.
     * It expects the header to be of the form:
     * <code>Header: arg-list</code>
     * 
     * @param parseLine - The header line
     */
    public void parseHeader(String parseLine) {
        StringTokenizer st = new StringTokenizer(parseLine, ": ");
        
        headerName = st.nextToken();
        if (headerName.equals("Date")) {
            
        }
        
        //Get the argument list
        while (st.hasMoreElements()) {
            String arg = st.nextToken();
            if (arg.length() > 0) {
                arg.trim();
                arg.replace(",", "");
                arg.trim();
                arguments.add(arg);
            }
        }
    }
    
    /**
     * 
     * Get's the header's name
     * 
     * @return - Header name
     */
    public String getHeaderName() {
        return headerName;
    }
    
    /**
     * 
     * Get's the header's argument List
     * 
     * @return - <code>List<String></code>
     */
    public List<String> getArguments() {
        return arguments;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o.getClass() != HTTPHeader.class)
            return false;
        else {
            HTTPHeader h = (HTTPHeader) o;
            if (!h.getHeaderName().equals(this.getHeaderName())) {
                return false;
            }
        }
        return true;
    }

}
