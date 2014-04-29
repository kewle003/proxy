package proxyTester;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.Test;

import proxy.ProxyServer;



public class ProxyTester {

    
    private static String CWD = System.getProperty("user.dir");
    @Test
    public void test() {
        ProxyServer server = new ProxyServer(CWD + "/src/proxy/config.txt", 9000);
        
        try {
            Socket clientSocket = new Socket("localhost", 9000);
            StringBuilder s = new StringBuilder("");
            s.append("GET file:///Users/mark/Documents/workspace/internet_programming/test.html HTTP/1.1\r\n");
            s.append("Host: localhost\r\n");
            s.append("Cache-Control: only-if-cached\r\n");
            s.append("Connection: close\r\n");
            s.append("\r\n");
            clientSocket.getOutputStream().write(s.toString().getBytes());
            
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
