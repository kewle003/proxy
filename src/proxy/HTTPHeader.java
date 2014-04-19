package proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class HTTPHeader {
    
    private String headerName;
    private List<String> arguments;
    
    public HTTPHeader() {
        headerName = new String("");
        arguments = new ArrayList<String>();
    }
    
    public void parseHeader(String parseLine) {
        StringTokenizer st = new StringTokenizer(parseLine, ": ");
        headerName = st.nextToken();
        
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
    
    public String getHeaderName() {
        return headerName;
    }
    
    public List<String> getArguments() {
        return arguments;
    }

}
