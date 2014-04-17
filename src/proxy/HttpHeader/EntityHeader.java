package proxy.HttpHeader;

import java.util.StringTokenizer;

public class EntityHeader extends HTTPHeader {
    enum EntityValue {
        Allow,
        Content_Encoding,
        Content_Language,
        Content_Length,
        Content_Location,
        Content_MD5,
        Content_Range,
        Content_Type,
        Expires,
        Last_Modified,
        extension_header
    }
    
    private EntityValue entVal;
    private String headerVal;
    
    public EntityHeader() {
         headerVal = "";
    }
    
    public String toString() {
        String retVal = entVal.toString();
        retVal = retVal.replace("_", "-");
        retVal = retVal.concat("=");
        retVal.concat(headerVal);
        return retVal;
    }

    @Override
    public boolean parseHeader(String stringToParse) {
        StringTokenizer st = new StringTokenizer(stringToParse);
        //Parse the header
        //First will be the entity-header value
        String valueToCheck = st.nextToken();
        
        //In case there is any white-space clear it
        valueToCheck.trim();
        entVal = EntityValue.valueOf(valueToCheck.replace("-", "_"));
        
        //Check if it was a valid value?
        
        //Somehow get the rest of the values??
        return true;
    }

}

