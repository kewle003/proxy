package proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigFile {
    
    private static String COMMENT = "#";

    /**
     * 
     * This HashMap holds on to the domain_name: www.example.com
     * and additional arguments (*, /img, /img/jpg, /img/*, etc..)
     * <domain_name, additional_args>
     */
    HashMap<String, List<String>> domains;
    
    /**
     * This function will read in the file that has
     * all the disallowed domains. It will popuate
     * the HashMap of domains.
     * 
     * @param filename
     */
    public ConfigFile(String filename) {
        domains = new HashMap<String,List<String>>();
        File f = new File(filename);
        String currentLine = new String();
        String domainName = new String();
        List<String> arguments;
        Pattern p = Pattern.compile("[a-zA-Z0-9]");
        
        try {
            FileReader fr = new FileReader(f);
            BufferedReader buf = new BufferedReader(fr);
            
            while ((currentLine = buf.readLine()) != null) {
                currentLine.trim();
                Matcher m = p.matcher(currentLine);
                
                /**
                 * Skip comments, null lines, and blank lines
                 */
                if (currentLine.startsWith(COMMENT) || currentLine.length() == 0 || !(m.find())) {
                    continue;
                }
                StringTokenizer st = new StringTokenizer(currentLine);
                
                String temp = st.nextToken();
                if (temp.startsWith("www") || temp.startsWith("http")) {
                    domainName = temp;
                }
                
                arguments = new ArrayList<String>();
                
                while (st.hasMoreElements()) 
                    arguments.add(st.nextToken());
                
                domains.put(domainName, arguments);
                
            }
            
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    
    
    /**
     * This returns the HashMap of the disallowed domains.
     * This will be useful when checking if the domain exists
     * and if so what we wish to block from it.
     * @return
     */
    public HashMap<String, List<String>> getDissallowedDomains() {
        return domains;
    }
    
    public List<String> createNewArgumentList(List<String> oldArgList, String key) {
        List<String> newArgList = new ArrayList<String>();
        ArrayList<String> dissAllowedArgs = (ArrayList<String>) domains.get(key);
        String allImages = "/img/*";
        boolean blockAllImages = false;
        if (dissAllowedArgs.contains(allImages)) {
            blockAllImages = true;
        }
        for (String arg : oldArgList) {
            if (blockAllImages) {
                if (!arg.contains("img")) {
                    newArgList.add(arg);
                }
            } else {
                if (!dissAllowedArgs.contains(arg)) {
                    newArgList.add(arg);
                }
            }
        }
        
        return newArgList;
    }
}
