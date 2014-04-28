package proxy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;


public class HTTPResponse {
    
    private int BUF_SIZE = 3000;
    
    private StringBuilder data;
    boolean isCacheAble;
    private int maxAge;
    private int maxStale;
    private boolean image = false;
    private boolean inValidContent = false;
    private boolean isText = false;
    private HttpURLConnection conn;
    private HTTPRequest httpReq;

    
    public HTTPResponse() {
        maxAge = 0;
        maxStale = 0;
    }
    
    public void parseResponse(HTTPRequest httpReqs) {
        try {
            httpReq = httpReqs;
            URL urlObj = new URL(httpReq.getURI());
            boolean https = false;
            boolean post = false;
            boolean redirect = false;
            boolean chunked = false;
            isCacheAble = false;
            
            if (httpReq.getPort() == 443) {
                https = true;
            } 
            
            if (!https) {
                conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod(httpReq.getMethod());
                conn.addRequestProperty("Connection", "close");

                //System.out.println("Response code for "+httpReq.getURI()+" : " +conn.getResponseCode());
                if (conn.getHeaderField("Content-Type") != null) {
                    String contentType = conn.getHeaderField("Content-Type");
                    if (contentType.contains("image")) {
                        image = true;
                        validateContentType(contentType);
                        return;
                    }
                }
                
                if (conn.getHeaderField("Cache-Control") != null) {
                   // System.out.println("Cache-Control Value: " +conn.getHeaderField("Cache-Control"));
                    String cacheVal = conn.getHeaderField("Cache-Control");
                    if (cacheVal.contains("no-cache") ||
                            cacheVal.contains("no-store") ||
                            cacheVal.contains("private")) {
                        isCacheAble = false;
                    } else {
                        isCacheAble = true;
                        parseCacheHeaderVal(conn.getHeaderField("Cache-Control"));
                    }
                }
                
                if (conn.getHeaderField("Transfer-Encoding") != null) {
                    String value = conn.getHeaderField("Transfer-Encoding");
                    if (value.equalsIgnoreCase("chunked"));
                    chunked = true;
                }
                
                //Uncomment for debugging
                //printHeaderValues(conn.getHeaderFields());
                
                
                //If POST set the DoOutput to true
                if (httpReq.getMethod().equalsIgnoreCase("POST")) {
                    conn.setDoOutput(true);
                    post = true;
                }
                
                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                            responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                        redirect = true;
                    }
                }
                
                
                if (redirect) {
                    System.out.println("REDIRECTION!!!!!!!!!!");
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
                    
                } 
                
                if (conn.getResponseCode() < 300) {
                    if (conn.getContentType().contains("text/")) {
                        isText = true;
                        if (!chunked) {                            
                            conn.connect();
                            //System.out.println("-----------NOT CHUNKED--------------");
                            BufferedReader inLine = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuffer init = new StringBuffer("");
                            String inputLine;
                            Pattern headReg = Pattern.compile("<\\s*head\\s*>");
                            while ((inputLine = inLine.readLine()) != null) {
                                Matcher m = headReg.matcher(inputLine);
                                if (m.find()) {
                                    inputLine = inputLine.replaceFirst("<\\s*head\\s*>", "<head><base href = \"" +httpReq.getURI()+ "\"/>");
                                }
                                init.append(inputLine);
                            }                    
                            data = new StringBuilder(new String(init.toString().getBytes(), "UTF-8")); 
                            inLine.close();
                        } else {
                            //System.out.println("--------------CHUNKED DATA------------");
                            conn.connect();
                            ByteArrayOutputStream sink = new ByteArrayOutputStream();
                            writeChunked(conn.getInputStream(), sink);  
                            data = new StringBuilder(new String(sink.toByteArray(),"UTF-8"));
                            Pattern headReg = Pattern.compile("<\\s*head\\s*>");
                            Matcher m = headReg.matcher(data.toString());
                            if (m.find()) {
                                data = new StringBuilder(data.toString().replaceFirst("<\\s*head\\s*>", "<head><base href = \"" +httpReq.getURI()+ "\"/>"));
                            }
                        }
                        conn.disconnect();
                    } else {
                        isText = false;
                    }
                } else {
                    System.out.println("FUUUUUUUUUUUUCCCKKKKKKKKKKKKK");
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
    
    private void validateContentType(String contentType) {
        for (String contentVal : httpReq.getDissAllowedMIME()) {
            if (contentType.contains(contentVal)) {
                inValidContent = true;
                return;
            }
        }
    }

    private void parseCacheHeaderVal(String headerField) {
        StringTokenizer st = new StringTokenizer(headerField);
        String value = new String("");
        while (st.hasMoreElements()) {
            value = st.nextToken();
           // System.out.println(value+ " ");
            if (value != null) {
                if (value.contains("max-age")) {
                    //System.out.println("MaxAge encountered!! " + value);
                    //System.out.println("Attempt" + value.substring(8));
                    maxAge = Integer.parseInt(value.substring(8))*1000;
                } else if (value.contains("max-stale")) {
                    //System.out.println("MaxStale Encountered!!" + value);
                    //System.out.println("Attempt" + value.substring(10));
                    maxStale = Integer.parseInt(value.substring(10))*1000;
                }
            }
        }
        
    }
    
    private String writeErrorMessage() {
        
        StringBuilder s = new StringBuilder("");
        s.append("HTTP/1.1 404 Not found");
        s.append("Content-Type: text/html; charset=ISO-8859-1");
        s.append("Connection: close");
        return s.toString();
    }

    /**
     * 
     * 
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
     * Method used for debugging purposes.
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

    public boolean isCacheAble() {
        return (isCacheAble && isText);
    }
    
    public String getData() {
        return data.toString();
    }
    
    public int getMaxAgeCache() {
        return maxAge;
    }
    
    public int getMaxStaleCache() {
        return maxStale;
    }
    
    public boolean isImage() {
        return image;
    }
    
    public boolean isInValidContent() {
        return inValidContent;
    }
    
    public HttpURLConnection getHttpUrlConnection() {
        return conn;
    }
    
    public boolean isText() {
        return isText;
    }

}
