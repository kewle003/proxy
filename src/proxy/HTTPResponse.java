package proxy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;


/**
 * 
 * This class handles everything to do with  an
 * HTTP Response for our Proxy Server.
 * 
 * @author mark
 *
 */
public class HTTPResponse {
    
    //The max buf size we wish to give
    private int BUF_SIZE = 3000;
    
    //The data stored from text/extension
    private StringBuilder data;
    
    //Boolean value to check if the response may be cached
    boolean isCacheAble;
    
    //Values used for Cache control
    private int maxAge;
    private int maxStale;
    
    //Boolean value to check if Content-Type is type image/extension
    private boolean image = false;
    
    //Boolean value to check if Content-Type is allowed by Proxy
    private boolean inValidContent = false;
    
    //Boolean value to check if Cont-Type is type text/extension
    private boolean isText = false;
    
    //A handle on an HttpURLConnection object
    private HttpURLConnection conn;
    
    //A handle on our HTTPRequest object
    private HTTPRequest httpReq;

    /**
     * 
     * Default constructor. Sets initial
     * max-age and max-stale values.
     * 
     */
    public HTTPResponse() {
        maxAge = 0;
        maxStale = 0;
    }
    
    /**
     * 
     * This method will parse the HTTP Response received by
     * the server based on the HTTP Request that the client
     * browser sends. It will check for valid content, cache
     * controls, response codes, as well as redirections.
     * 
     * @param httpReqs
     */
    public void parseResponse(HTTPRequest httpReqs) {
        try {
            httpReq = httpReqs;
            URL urlObj = new URL(httpReq.getURI());
            boolean https = false;
            boolean redirect = false;
            boolean chunked = false;
            isCacheAble = false;
            
            if (httpReq.getPort() == 443 || httpReq.getURI().contains("https")) {
                https = true;
            } 
            
            //http protocol?
            if (!https) {
                //Create an HttpURLConnection object
                conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod(httpReq.getMethod());
                conn.addRequestProperty("Connection", "close"); /* Professor Tripathi said to work with this */
                
                if (httpReq.isCookieSet()) {
                    if (httpReq.getCookieData() != null) {
                        System.out.println(httpReq.getCookieData());
                        conn.addRequestProperty("Set-Cookie", httpReq.getCookieData());
                    }
                }
                
                //Verify that the image/extension is valid
                if (conn.getHeaderField("Content-Type") != null) {
                    String contentType = conn.getHeaderField("Content-Type");
                    if (contentType.contains("image")) {
                        image = true;
                        validateContentType(contentType);
                        return;
                    }
                }
                
                //Check the Cache-Control header
                if (conn.getHeaderField("Cache-Control") != null) {
                    String cacheVal = conn.getHeaderField("Cache-Control");
                    //Are we capable of caching?
                    if (cacheVal.contains("no-cache") ||
                            cacheVal.contains("no-store") ||
                            cacheVal.contains("private")) {
                        isCacheAble = false;
                    } else {
                        isCacheAble = true;
                        //parse out max-age and max-stale
                        parseCacheHeaderVal(conn.getHeaderField("Cache-Control"));
                    }
                }
                
                //Check if we need to read the data as a chunked data
                if (conn.getHeaderField("Transfer-Encoding") != null) {
                    String value = conn.getHeaderField("Transfer-Encoding");
                    if (value.equalsIgnoreCase("chunked"));
                    chunked = true;
                }
                
                //Uncomment for debugging
                printHeaderValues(conn.getHeaderFields());
                
                
                //If POST set the DoOutput to true
               // if (httpReq.getMethod().equalsIgnoreCase("POST")) {
                 //   String cookies = conn.getHeaderField("Set-Cookie");
                   // System.out.println(cookies);
                   // conn.setDoOutput(true);
                //}
                
                int responseCode = conn.getResponseCode();
                //Check if we need to redirect
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                            responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                        redirect = true;
                    }
                }
                
                //If we encounter a redirection
                if (redirect) {
                    //Grab the redirection url
                    String redirectUrl = conn.getHeaderField("Location");
                    
                    //Grab the cookies if any
                    String cookies = conn.getHeaderField("Set-Cookie");
                    
                    //Open a new URL connection
                    conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
                    conn.setRequestProperty("Cookie", cookies);
                    conn.setRequestProperty("Accept-Language", "en-US,en;q=0.8");
                    conn.setRequestProperty("User-Agent", "Mozilla");
                    conn.addRequestProperty("Connection", "close");
                    conn.addRequestProperty("Referer", httpReq.getURI());
                    System.out.println("REDIRECTED:" +redirectUrl);
                    
                    if (redirectUrl.contains("https")) {
                        https = true;
                    }
                    
                } 
                
                //Do we have a valid response?
                if (conn.getResponseCode() < 300) {
                    if (conn.getContentType().contains("text/")) {
                        isText = true;
                        if (!chunked) {                            
                            conn.connect();
                            //Create our InputSocket buffer
                            BufferedReader inLine = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuffer init = new StringBuffer("");
                            String inputLine;
                            Pattern headReg = Pattern.compile("<\\s*head\\s*>");
                            while ((inputLine = inLine.readLine()) != null) {
                                Matcher m = headReg.matcher(inputLine);
                                //Check if <head> is in the string, if so set the <base> tag href
                                if (m.find()) {
                                    inputLine = inputLine.replaceFirst("<\\s*head\\s*>", "<head><base href = \"" +httpReq.getURI()+ "\"/>");
                                }
                                init.append(inputLine);
                            }                    
                            data = new StringBuilder(new String(init.toString().getBytes(), "UTF-8")); 
                            inLine.close();
                        } else {
                            conn.connect();
                            ByteArrayOutputStream sink = new ByteArrayOutputStream();
                            //Write the chunked data
                            writeChunked(conn.getInputStream(), sink);  
                            data = new StringBuilder(new String(sink.toByteArray(),"UTF-8"));
                            Pattern headReg = Pattern.compile("<\\s*head\\s*>");
                            Matcher m = headReg.matcher(data.toString());
                            //Check if <head> is in the string, if so set the <base> tag href
                            if (m.find()) {
                                data = new StringBuilder(data.toString().replaceFirst("<\\s*head\\s*>", "<head><base href = \"" +httpReq.getURI()+ "\"/>"));
                            }
                        }
                        conn.disconnect();
                    } else {
                        isText = false;
                    }
                }
            } else {
                System.out.println("https!!!!!!!!!!!!");
                HttpsURLConnection conn = (HttpsURLConnection) urlObj.openConnection();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 
     * This method will validate an image/extension
     * type. If it is a type that we wish to block
     * we must notify the HTTPResponse object.
     * 
     * @param contentType
     */
    private void validateContentType(String contentType) {
        for (String contentVal : httpReq.getDissAllowedMIME()) {
            if (contentVal.equals("star")) {
                inValidContent = true;
                return;
            } else if (contentType.contains(contentVal)) {
                inValidContent = true;
                return;
            }
        }
    }

    /**
     * 
     * This method will take the Cache-Control header field
     * and will parse out max-age and max-stale.
     * 
     * @param headerField
     */
    private void parseCacheHeaderVal(String headerField) {
        StringTokenizer st = new StringTokenizer(headerField);
        String value = new String("");
        while (st.hasMoreElements()) {
            value = st.nextToken();
            if (value != null) {
                if (value.contains("max-age")) {
                    maxAge = Integer.parseInt(value.substring(8))*1000;
                } else if (value.contains("max-stale")) {
                    maxStale = Integer.parseInt(value.substring(10))*1000;
                }
            }
        }
        
    }

    /**
     * 
     * This method is used for writing rawdata from the
     * server's InputStream to the client's ByteArrayOutputStream buffer.
     * This is used to store data if it is chunked.
     * 
     * @param istream
     * @param ostream
     * @throws Exception
     */
    private void writeChunked(InputStream istream, ByteArrayOutputStream ostream) throws Exception {
        // Read bytes and write to destination until eof
        byte[] buf = new byte[BUF_SIZE];
        int len = 0;
        
        while ((len = istream.read(buf)) >= 0) {
            ostream.write(buf, 0, len);
        }  
        istream.close();
        ostream.flush();
        ostream.close();
    }

    /**
     * 
     * This is used for debugging. It will grab
     * the Response headers and print out the values
     * in an Array.
     * 
     * @param headerFields
     */
    public void printHeaderValues(Map<String, List<String>> headerFields) {
        System.out.println("------------HEADER DATA----------");
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
            System.out.println(headerFieldKey + "=" + sb.toString());
        }
        System.out.println("------------END DATA----------");
    }

    /**
     * 
     * Checks if the Response is cacheable.
     * 
     * @return true - it can be cached, false otherwise.
     */
    public boolean isCacheAble() {
        return (isCacheAble && isText);
    }
    
    /**
     * 
     * This method will grab the
     * stored data.
     * 
     * @return String - data
     */
    public String getData() {
        return data.toString();
    }
    public int getMaxAgeCache() {
        return maxAge;
    }
    
    public int getMaxStaleCache() {
        return maxStale;
    }
    
    /**
     * 
     * Method used to verify if the
     * Content-Type is of type image/*
     * 
     * @return true - is an image, false otherwise
     */
    public boolean isImage() {
        return image;
    }
    
    /**
     * 
     * Method used to check if the Content-Type
     * is allowed by our Config File specifications.
     * 
     * @return true - is allowed, false otherwise
     */
    public boolean isInValidContent() {
        return inValidContent;
    }
    
    /**
     * 
     * This method will retrieve the HttpURLConnection
     * to the host.
     * 
     * @return
     */
    public HttpURLConnection getHttpUrlConnection() {
        return conn;
    }
    
    /**
     * 
     * This method will check if the
     * Content-Type is of type text/*
     * 
     * @return true - if so, false otherwise
     */
    public boolean isText() {
        return isText;
    }

}
