package proxy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.DefaultCaret;

/**
 * 
 * This class sets up the GUI that interacts
 * with our main ProxyServerEngine. It begins by setting
 * up a JFrame. From here it requires the username
 * 
 * @author mark
 *
 */
public class ProxyServer {
    
    private static String CWD = System.getProperty("user.dir");
    private static String OS = System.getProperty("os.name").toLowerCase();
    
    private JTextField portText;
    private JTextField configText;
    private JFrame frame;
    
    private JButton startButton;
    
    private PrintStream out;
    
    private ProxyServerEngine proxyServer;
    
    public static void main(String[] args) throws InterruptedException {
        ProxyServer gui = new ProxyServer();
        gui.go();
    }
    
    /**
     * 
     * Event listener for our Start button. It will
     * start up the ProxyServer when a user enters valid,
     * input. Valid input must be a .txt file and a valid
     * port number in the range 1024-65535.
     * 
     * @author mark
     *
     */
    private class StartButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            //Was there valid input?
            if (configText.getText() != null || portText.getText() != null) {
                String portToCheck = portText.getText();
                Pattern p = Pattern.compile("\\d\\d\\d\\d\\d?\\b");
                Matcher m = p.matcher(portToCheck);
                //Verify we have at least a 4 digit integer and no more than 5
                if (!m.find()) {
                    System.out.println(""+portToCheck+ " is invalid, please insert a port number: 1024-65535");
                    return;
                }
                String configFileToCheck = configText.getText();
                Pattern p2 = Pattern.compile("[a-zA-Z]*\\.txt");
                Matcher m2 = p2.matcher(configFileToCheck);
                //Verify we have a text file
                if (!m2.find()) {
                    System.out.println(""+configFileToCheck+ " is invalid, please insert a config file: example.txt");
                    return;
                }
                //Verify the port is a valid
                int port = Integer.parseInt(portToCheck);
                if (port < 1024) {
                    System.out.println("This port number is reserved, please insert a valid port");
                    return;
                }
                startButton.setEnabled(false);
                portText.setEnabled(false);
                configText.setEnabled(false);
                if (OS.contains("windows")) {
                    proxyServer = new ProxyServerEngine(CWD+"\\"+configFileToCheck, port);
                } else {
                    proxyServer = new ProxyServerEngine(CWD+"/"+configFileToCheck, port);
                }
                System.out.println("Starting server on port: " +port);
                proxyServer.start();
            } else {
                System.out.println("Please enter valid input");
            }
        }
    }
    
    /**
     * 
     * Event listener for when the Stop button is
     * pressed on our GUI. It will close the frame and
     * exit the system.
     * 
     * @author mark
     *
     */
    private class StopButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            frame.dispose();   
            System.exit(0);
        }
    }

    /**
     * 
     * Main logic to setting up our GUI
     * 
     * @throws InterruptedException
     */
    public void go() throws InterruptedException {
        frame = new JFrame("Proxy Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create the user input features
        JPanel inputPanel = new JPanel();
        portText = new JTextField("9000");
        configText = new JTextField("config.txt");
        JLabel portLabel = new JLabel("Enter a portNumber [1024-65535]");
        JLabel configLabel = new JLabel("Enter a config file (.txt)");
        configLabel.setLabelFor(configText);
        portLabel.setLabelFor(portText);
        portText.setColumns(10);
        configText.setColumns(10);
        inputPanel.add(portLabel);
        inputPanel.add(portText);
        inputPanel.add(configLabel);
        inputPanel.add(configText);
        
        //Create the command line text area
        JTextArea textArea = new JTextArea();
        textArea.setForeground(Color.RED);
        textArea.setBackground(Color.BLACK);
        //Always update when append() is used
        DefaultCaret caret = (DefaultCaret)textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scroller = new JScrollPane(textArea);
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        out = new PrintStream(new TextAreaOutputStream(textArea));
        System.setOut(out);
        System.setErr(out);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        //Follow the text
        scroller.setViewportView(textArea);

        //Create the start button
        JPanel buttonPanel = new JPanel();
        JButton quitButton = new JButton("Quit");
        startButton = new JButton("Start Server");
        startButton.addActionListener(new StartButtonListener());
        quitButton.addActionListener(new StopButtonListener());
        buttonPanel.add(startButton);
        buttonPanel.add(quitButton);
        
        //Register all the Components
        frame.getContentPane().add(BorderLayout.SOUTH, buttonPanel);
        frame.getContentPane().add(BorderLayout.NORTH, inputPanel);
        frame.getContentPane().add(BorderLayout.CENTER, scroller);
        frame.setBackground(Color.DARK_GRAY);
        frame.setSize(800, 700);
        frame.setVisible(true);
        
        System.out.println("Welcome, we expect the config file to be in "+CWD);
        
    }
}
