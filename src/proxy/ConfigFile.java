package proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class ConfigFile {
    
    private static String WILDCARD = "*";
    private static String COMMENT = "#";

    /**
     * 
     * This HashMap holds on to the domain_name: www.example.com
     * and additional arguments (*, /img, /img/jpg, /img/*, etc..)
     * <domain_name, additional_args>
     */
    HashMap<String, String> domains;
    
    /**
     * This function will read in the file that has
     * all the disallowed domains. It will popuate
     * the HashMap of domains.
     * 
     * @param filename
     */
    public ConfigFile(String filename) {
        domains = new HashMap<String,String>();
        File f = new File(filename);
        String currentLine = new String();
        String domainName = new String();
        String arguments = new String();
        
        try {
            FileReader fr = new FileReader(f);
            BufferedReader buf = new BufferedReader(fr);
            
            while ((currentLine = buf.readLine()) != null) {
                currentLine.trim();
                
                if (currentLine.startsWith(COMMENT) || currentLine.length() == 0) {
                    continue;
                } else {
                    //Split up the string
                }
                
                
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public HashMap<String, String> getDissallowedDomains() {
        return domains;
    }
}
