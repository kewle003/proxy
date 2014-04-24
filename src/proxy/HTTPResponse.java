package proxy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * This class will store all the header values for
 * an HTTP response.
 * 
 * @author mark
 *
 */
public class HTTPResponse {
    
    private List<HTTPHeader> headerList;
    private ByteArrayOutputStream data;
    private int BUF_SIZE = 8096;
    
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
        }
        URL u = new URL(uri);
        URLConnection conn = u.openConnection();
           
        //get all headers
       // System.out.println("******** Recieved Headers for debugging  **************");
        Map<String, List<String>> headerFields = conn.getHeaderFields();
   
        Set<String> headerFieldsSet = headerFields.keySet();
        Iterator<String> hearerFieldsIter = headerFieldsSet.iterator();

        while (hearerFieldsIter.hasNext()) {
            String headerFieldKey = hearerFieldsIter.next();
            List<String> headerFieldValue = headerFields.get(headerFieldKey);
            List<String> arguments = new ArrayList<String>();
                        
            StringBuilder sb = new StringBuilder();
    
            for (String value : headerFieldValue) {
                value.trim();
                sb.append(value);
                sb.append("");
                //arguments.add(value);
            }
            StringTokenizer st = new StringTokenizer(sb.toString());
            while (st.hasMoreElements()) {
                String result = st.nextToken();
                if (result.contains(","))
                   result = result.substring(0, result.lastIndexOf(","));
                result.trim();
                arguments.add(result);
            }
            //System.out.println(headerFieldKey + "=" + sb.toString());
            headerList.add(new HTTPHeader(headerFieldKey, arguments)); 
        }
        //System.out.println("******** End of Recieved Headers for debugging  **************");
        
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
                    //System.out.println("getValueOfReq --- HeaderName: " +headerName);
                    //System.out.println("Values: " +header.getArguments());
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
                   // System.out.println("Checking ARGUMENTS: " +list.toString());
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
    
    /**
     * 
     * This method will grab the value of max-age if
     * it exists in milliseconds.
     * 
     * @return age in milliseconds if it exists, -1 if not.
     */
    public long getMaxAge() {
        for (HTTPHeader header : headerList) {
            if (header.getHeaderName() != null) {
                if (header.getHeaderName().equals("Cache-Control")) {
                    ArrayList<String> list = (ArrayList<String>) header.getArguments();
                    if (list != null) {
                        for (String element : list) {
                            if (element.startsWith("max-age")) {
                                return Integer.parseInt(element.substring(8))*1000;
                                }
                        }
                    }
                }
            }
        }
        return -1;
    }
    
    /**
     * 
     * This method will grab the value of max-stale
     * if it exists in milliseconds.
     * 
     * @return age in milliseconds if it exists, -1 if not.
     */
    public long getMaxStale() {
        for (HTTPHeader header : headerList) {
            if (header.getHeaderName() != null) {
                if (header.getHeaderName().equals("Cache-Control")) {
                    ArrayList<String> list = (ArrayList<String>) header.getArguments();
                    if (list != null) {
                        for (String element : list) {
                            if (element.startsWith("max-stale")) {
                                return Integer.parseInt(element.substring(10))*1000;
                                }
                        }
                    }
                }
            }
        }
        return -1;
    }
    
    public List<HTTPHeader> getHeaders() {
        return headerList;
    }
    
    public byte[] getResponseData() {
        return data.toString().getBytes();
    }

}
