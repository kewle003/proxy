package proxy;

import java.util.ArrayList;
//import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 * 
 * This class represents any HTTP header as an object.
 * It contains the header name as well as it's argument
 * list stored as <code>List<String></code>.
 * 
 * FIELD-NAME ":" [FIELD-VALUE] CRLF
 * 
 * @author mark
 *
 */
public class HTTPHeader {
    
    private String headerName;
    private List<String> arguments;
    //private Date cacheAge;
    
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
    
    public void setArguments(List<String> newArgList) {
        arguments = newArgList;
    }
    
    public String toString() {
        StringBuilder s;
        if (headerName != null)
            s = new StringBuilder(headerName+": ");
        else {
            s = new StringBuilder("");
            for (String args : arguments) {
                s.append(args + " ");
            }
        }
        if (headerName != null) {
            if (headerName.equalsIgnoreCase("Content-Type")) {
                s.append("text/xml; charset=utf-8");
            } else {
                for (String args : arguments) {
                    s.append(args + ",");
                }  
            }
        }
        
        //Remove last , on arg list
        if (s.lastIndexOf(",") > 0)
            s.deleteCharAt(s.lastIndexOf(","));
        //Append the CRF
        s.append("\r\n");
        return s.toString();
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
