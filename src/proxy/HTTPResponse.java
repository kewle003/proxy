package proxy;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class will store all the header values for
 * an HTTP response.
 * 
 * @author mark
 *
 */
public class HTTPResponse {
    
    private List<HTTPHeader> headerList;
    
    public HTTPResponse() {
        headerList = new ArrayList<HTTPHeader>();
    }
    
    /**
     * 
     * This method will parse the headers for an
     * HTTP response. It will build the headers and store
     * them into an ArrayList.
     * 
     * @param uri
     * @throws Exception
     */
    public void parseHeaders(String uri) throws Exception {
        if (!uri.startsWith("http:")) {
            uri = "http://".concat(uri);
        } else {
           URL u = new URL(uri);
           URLConnection conn = u.openConnection();
           
           //get all headers
           System.out.println("******** Recieved Headers for debugging  **************");
           Map<String, List<String>> headerFields = conn.getHeaderFields();
   
           Set<String> headerFieldsSet = headerFields.keySet();
           Iterator<String> hearerFieldsIter = headerFieldsSet.iterator();

           while (hearerFieldsIter.hasNext()) {
               String headerFieldKey = hearerFieldsIter.next();
               List<String> headerFieldValue = headerFields.get(headerFieldKey);
               List<String> arguments = new ArrayList<String>();
                        
               StringBuilder sb = new StringBuilder();
    
               for (String value : headerFieldValue) {
                   sb.append(value);
                   sb.append("");
                   arguments.add(value);
               }
               //System.out.println(headerFieldKey + "=" + sb.toString());
               headerList.add(new HTTPHeader(headerFieldKey, arguments)); 
           }
           System.out.println("******** End of Recieved Headers for debugging  **************");
           //System.out.println("******** BEGIN OF STORED HEADERS FOR DEBUG *******************");
           //for (HTTPHeader header : headerList) {
             //  System.out.print(header.getHeaderName()+ "= ");
               //for (String args : header.getArguments()) {
                 //  System.out.print(args+ ", ");
              // }
              // System.out.print("\n");
          // }
          // System.out.println("*********** END OF STORED HEADERS FOR DEBUG *******************");
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
       // System.out.println("getValueOfReq --- HeaderName: " +headerName);
        for (HTTPHeader header : headerList) {
           // System.out.println("getValueOfReq --- " +headerName+ " ==   " +header.getHeaderName());
            if (header.getHeaderName() != null) {
                if (header.getHeaderName().equals(headerName)) {
                    System.out.println("getValueOfReq --- HeaderName: " +headerName);
                    System.out.println("Values: " +header.getArguments());
                    return header.getArguments();
                }
            }
        }
        
        return null;
    }

    /**
     * 
     * This method will determine whether or not the
     * response my be cached for future use.
     * 
     * @return
     */
    public boolean isCacheable() {
        for (HTTPHeader header : headerList) {
            if (header.getHeaderName() != null) {
                if (header.getHeaderName().equals("Cache-Control")) {
                    ArrayList<String> list = (ArrayList<String>) header.getArguments();
                    if (list != null) {
                        if (list.contains("no-cache")) {
                            return false;
                        } else if (list.contains("proxy-revalidate")) {
                            return false;
                        } else if (list.contains("must-revalidate")) {
                            return false;
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
    public List<HTTPHeader> getHeaders() {
        return headerList;
    }

}
