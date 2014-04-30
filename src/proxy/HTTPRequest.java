package proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * 
 * This class handles everything to do with
 * an HTTP request from the client browser.
 * 
 * @author mark
 *
 */
public class HTTPRequest {

    //The client sockets InputStream
    private InputStream istream;
    
    //A handle on our client socket
    private Socket socket;
    
    //The data from an HTTP request
    private StringBuffer dataBuf;
    
    //A handle on the Host header field value
    private String host;
    
    //The port number for the requested server
    private int port;
    
    //HTTP1.0/1.1
    private String protocol;
    
    //Handle on the requested URL
    private String uri;
    
    //Handle on GET/POST/HEAD
    private String method;
    
    //The value from the Referrer header
    private String referer;
    
    //A handle on the dissAllowed image/extensions
    List<String> dissAllowedMimes;
    
    //Boolean value to check if only-if-cached is specified
    private boolean onlyIfCached = false;

    //Boolean value to check if Cookie header existed
    private boolean cookie = false;
    
    private String urlParameters;
    
    private boolean urlParametersSet;
    
    private String cookieData;
    
    /**
     * 
     * Enum that handles the image/extensions
     * 
     * @author mark
     *
     */
    public enum MIME_TYPE {
        fif,
        x_icon,
        gif,
        ief,
        ifs,
        jpeg,
        png,
        tiff,
        vnd,
        wavelet,
        bmp,
        x_photo_cd,
        x_cmu_raster,
        x_portable_anymap,
        x_portable_bitmap,
        x_portable_graymap,
        x_portable_pixmap,
        x_rgb,
        x_xbitmap,
        x_xpixmap,
        x_xwindowdump,
        star,
        none, 
        all
        
    }
    
    /**
     * 
     * Default constructor. This will take in a
     * client socket and grab it's InputStream.
     * 
     * @param socket - socket to the client browser
     */
    public HTTPRequest(Socket socket) {
        this.socket = socket;
        try {
            this.istream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataBuf = new StringBuffer("");
        parseData(istream);
    }

    /**
     * 
     * This method will parse an HTTP Request that
     * was sent from the browser.
     * 
     * @param inputStream
     */
    private void parseData(InputStream inputStream) {
        BufferedReader inLine = null;
        try {
            inLine = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        String rawData = new String("");
        try {
            rawData = inLine.readLine();
            if (rawData == null)
                return;
            // Parse the first request line
            parseRequestLine(rawData);
            //URL u = new URL(uri);
            //URLConnection conn = uri.openConnection();
            //System.out.println(u.getQuery());
            if (method.equals("GET")) {
                while (rawData.length() > 0) {
                    //Host: line?
                    if (rawData.contains("Host")) {
                        parseHost(rawData);
                    }
                    //Referer: line?
                    if (rawData.contains("Referer")) {
                        parseReferer(rawData);
                    }
                    //Cache-Control line?
                    if (rawData.contains("only-if-cached")) {
                        onlyIfCached = true;
                    }
                    if (rawData.contains("Cookie")) {
                        parseCookie(rawData);
                        cookie  = true;
                    }
                    //System.out.println(rawData);
                    dataBuf.append(rawData + "\r\n");
                    rawData = inLine.readLine();
                }
                dataBuf.append("\r\n");
            } else {
                char[] buffer = new char[8192];
                int count = 1;
               // StringBuilder s = new StringBuilder("");
                while (inLine.ready()) {
                    count = inLine.read(buffer, 0, 8192);
                    if (count > 0) {
                        //System.out.println("In Loop");
                        dataBuf.append(buffer, 0, count);
                    } else
                        break;
                    
                }
                parsePostParameters(dataBuf.toString());
                //System.out.println("Out of while loop");
                //System.out.println(dataBuf.toString());
                
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        }
       // dataBuf.append("\r\n");
    }
    

    private void parsePostParameters(String string) {
        BufferedReader inLine = new BufferedReader(new StringReader(string));
        String rawData = new String("");
        int length = 0;
        try {
            while ((rawData = inLine.readLine()) != null) {
              //Host: line?
                if (rawData.contains("Host")) {
                    parseHost(rawData);
                }
                //Referer: line?
                if (rawData.contains("Referer")) {
                    parseReferer(rawData);
                }
                //Cache-Control line?
                if (rawData.contains("only-if-cached")) {
                    onlyIfCached = true;
                }
                if (rawData.contains("Cookie")) {
                    parseCookie(rawData);
                    cookie  = true;
                }
                if (rawData.contains("Content-Length")) {
                    StringTokenizer st = new StringTokenizer(rawData);
                    st.nextToken(); //Ignore Content-Length;
                    length = Integer.parseInt(st.nextToken());
                }
                if (length>0) {
                    if (rawData.length() == length) {
                        urlParameters = rawData;
                        urlParametersSet = true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * 
     * 
     * @param rawData
     */
    private void parseCookie(String rawData) {
        StringTokenizer st = new StringTokenizer(rawData);
        st.nextToken(); /* get rid of cookie header */
        StringBuilder cookieBuilder = new StringBuilder("");
        while (st.hasMoreTokens()) {
            cookieBuilder.append(st.nextToken());
        }
        cookieData = new String(cookieBuilder.toString());
        
    }

    /**
     * 
     * This method will parse the request-line which
     * is the first line in an HTTP Request.
     * 
     * @param rawData
     */
    private void parseRequestLine(String rawData) {
        StringTokenizer st = new StringTokenizer(rawData);
        try {
            method = st.nextToken();
            uri = st.nextToken();
            protocol = st.nextToken();
        } catch (Exception NoSuchElement) {
        }
            
    }

    /**
     * 
     * This method will parse the Host: header field.
     * 
     * @param rawData
     */
    private void parseHost(String rawData) {
        StringTokenizer st = new StringTokenizer(rawData, ": ");
        if (st.nextToken().equals("Host")) {
            host = st.nextToken();
            String portString = new String("");
            try {
                portString = st.nextToken();
            } catch (Exception NoSuchElement) {
            }
            if (portString.length() == 0) {
                port = 80;
            } else {
                port = Integer.parseInt(portString);
            }
        }
    }
    
    /**
     * 
     * This will parse the Referer: header field.
     * 
     * @param data
     */
    private void parseReferer(String data) {
        StringTokenizer st = new StringTokenizer(data);
        st.nextToken(); /* Ignore Referer*/
        referer = st.nextToken();
    }
    
    /**
     * 
     * This method will set the disallowed types from
     * the Content-Type: image/extension from an HTTP Response.
     * The disallowed times are from the ConfigFile.
     * 
     * @param list
     */
    public void setDissAllowedMIME(List<String> list) {
        dissAllowedMimes = new ArrayList<String>();
      //If there is nothing here => there is nothing to block
        if (list == null) {
            dissAllowedMimes.add(MIME_TYPE.none.toString().toLowerCase());
            return;
        }
        
        
        if (list.contains("image/*")) {
            dissAllowedMimes.add(MIME_TYPE.star.toString().toLowerCase());
        } else if (list.size() == 0) {
            dissAllowedMimes.add(MIME_TYPE.all.toString().toLowerCase());
        } else if (list.contains("*")) {
            dissAllowedMimes.add(MIME_TYPE.all.toString().toLowerCase());
        } else {
            if (list.contains("image/jpeg") || list.contains("image/jpg")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.jpeg.toString().toLowerCase());
            }
            
            if (list.contains("image/bmp")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.bmp.toString().toLowerCase());
            }
            
            if (list.contains("image/gif")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.gif.toString().toLowerCase());
            }
            
            if (list.contains("image/png")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.png.toString().toLowerCase());
            }
            
            if (list.contains("image/fif")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.fif.toString().toLowerCase());
            }
            
            if (list.contains("image/x-icon")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.x_icon.toString().replaceAll("_", "-").toLowerCase());
            }

            if (list.contains("image/ief")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.ief.toString().toLowerCase());
            }
            
            if (list.contains("image/ifs")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.ifs.toString().toLowerCase());
            }
            
            if (list.contains("image/vnd")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.vnd.toString().toLowerCase());
            }
            
            if (list.contains("image/wavelet")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.wavelet.toString().toLowerCase());
            }
            
            if (list.contains("image/x-photo-cd")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.x_photo_cd.toString().replaceAll("_", "-").toLowerCase());
            }
            
            if (list.contains("image/x-cmu-raster")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.x_cmu_raster.toString().replaceAll("_", "-").toLowerCase());
            }
            
            if (list.contains("image/x-portable-anymap ")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.x_portable_anymap.toString().replaceAll("_", "-").toLowerCase());
            }
            
            if (list.contains("image/x-portable-bitmap ")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.x_portable_bitmap.toString().replaceAll("_", "-").toLowerCase());
            }
            
            if (list.contains("image/x-portable-graymap")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.x_portable_graymap.toString().replaceAll("_", "-").toLowerCase());
            }
            
            if (list.contains("image/x-portable-pixmap")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.x_portable_pixmap.toString().replaceAll("_", "-").toLowerCase());
            }
            
            if (list.contains("image/x-rgb")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.x_rgb.toString().replaceAll("_", "-").toLowerCase());
            }
            
            if (list.contains("image/x-xbitmap")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.x_xbitmap.toString().replaceAll("_", "-").toLowerCase());
            }
            
            if (list.contains("image/x-xpixmap")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.x_xpixmap.toString().replaceAll("_", "-").toLowerCase());
            }
            
            if (list.contains("image/x-xwindowdump")) {
                dissAllowedMimes.add("image/" + MIME_TYPE.x_xwindowdump.toString().replace("_", "-").toLowerCase());
            }
        }
    }
    
    public String getData() {
        return dataBuf.toString();
    }
    
    public int getPort() {
        return port;
    }
    
    public String getHost() {
        return host;
    }
    
    public String getMethod() {
        return method;
    }
    
    public String getURI() {
        return uri;
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public String getReferer() {
        return referer;
    }
      
    public Socket getClientSocket() {
        return socket;
    }
    
    public List<String> getDissAllowedMIME() {
        return dissAllowedMimes;
    }
    
    public boolean onlyIfCacheSet() {
        return onlyIfCached;
    }
    
    public boolean isCookieSet() {
        return cookie;
    }
    
    public String getCookieData() {
        return cookieData;
    }
    
    public String getUrlParameters() {
        return urlParameters;
    }
    
    public boolean isUrlParametersSet() {
        return urlParametersSet;
    }

}
