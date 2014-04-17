package proxy.HttpHeader;

/**
 * This class will handle setting up an HTTP Header.
 * @author mark
 *
 */
public abstract class HTTPHeader {
    
    /**
     * 
     * This method will parse the header
     * 
     * @param stringToParse
     * @return true if successful, false otherwise
     */
    public abstract boolean parseHeader(String stringToParse);
    
    /**
     * 
     * This method will return the string value of the header
     * 
     */
    public abstract String toString();
}
