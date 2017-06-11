/*
 * /home/grant/JavaClasses/Comp213/MetaClient/MetaClient.java
 *
 * Created: Wed Oct 21 23:49:50 2015
 *
 * copyright Grant Malcolm
 *
 *   This source code may be freely used, modified, or distributed
 *   provided due credit is given.
 *
 */

package Comp213.MetaClient;

import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import Comp213.FriendFaceClient.FriendFaceGUI;

/**
   Tool to show traffic between a server and a client.
   MetaClient displays the client GUI and acts as go-between
   for requests from the client GUI and the server:
   the client sends requests to MetaClient, which forwards them
   to the server, and displays the requests and the responses.
   It is assumed that all traffic between client and server
   is character-oriented.
   Controls are provided to allow the user to send requests directly
   without using the client GUI.
   <p>
   The client should implement the
   {@link ProxyableComponent ProxyableComponent} interface.
   </p>

   @author <a href="mailto:grant@liverpool.ac.uk">Grant Malcolm</a>
   @version 0.0
 */
public class MetaClient
   extends JFrame
   implements Proxy, WindowListener {

   /**
      Class to handle the client's interactions with the server,
      and to provide controls to connect to the server.
      Controls allow the user to specify the server's address,
      open and close a {@link java.net.Socket Socket} connection to the server,
      and to send data to the server.
      This class maintains both a persistent connection to the server,
      and a non-persistent connection that is closed after each
      {@link #handleRequest(String) interaction with the server}.
      The chief requirement is that {@link isConnected() isConnected()}
      always returns correct information.
    */
   private class ConnectorPanel extends JPanel {

      // --- GUI components ---------------------------------------------------

      /**
         JTextField for user to specify remote machine to connect to.
       */
      private final JTextField hostField = new JTextField(20);

      /**
         JTextField to specify port number of the server.
       */
      private final JTextField portField = new JTextField(20);

      /**
         JRadioButton to connect to the server.
         Initially set to no-connection
       */
      private final JRadioButton connectB = new JRadioButton("Connect", false);

      /**
         JTextArea for user to enter text to be sent to the server.
       */
      private final JTextArea textToSend = new JTextArea(5, 25);

      /**
         JButton to send {@link #textToSend user-supplied text} to server.
       */
      private final JButton sendButton = new JButton("Send text to Server");

      /**
         Display status messages.
         Newlines not displayed explicitly.
       */
      private final DisplayPanel status = new DisplayPanel(12, 25, false);

      // --- GUI methods ------------------------------------------------------

      /**
         Create the JPanel that allows the user to specify the
         address and port number of the server to connect to.

         @return the JPanel with controls to specify host address and
                 port number
       */
      private JPanel makeAddressPanel() {
         //JPanel to return
         final JPanel addressP = new JPanel();
         // with GridBagLayout to format labels and JTextFields
         addressP.setLayout(new GridBagLayout());

         // Constraints for the components
         final GridBagConstraints grid = new GridBagConstraints();
         // padding around components
         grid.ipadx = 2;
         grid.ipady = 2;

         /*  Row 1: "Server host:" + JTextField
          */
         final JLabel remoteLabel = new JLabel("Server host:");
         grid.gridx = 0; // first column
         grid.gridy = 0; // first row
         grid.fill = GridBagConstraints.HORIZONTAL;
         addressP.add(remoteLabel, grid); // add host label

         grid.gridx = 1; // second column
         grid.gridy = 0; // first row
         grid.fill = GridBagConstraints.HORIZONTAL;
         addressP.add(hostField, grid); // add host textfield

         /*  Row 2: "Port number:" + JTextField
          */
         final JLabel portLabel = new JLabel("Port number:");
         grid.gridx = 0; // first column
         grid.gridy = 1; // second row
         grid.fill = GridBagConstraints.HORIZONTAL;
         addressP.add(portLabel, grid); // add port number label

         grid.gridx = 1; // second column
         grid.gridy = 1; // second row
         grid.fill = GridBagConstraints.HORIZONTAL;
         addressP.add(portField, grid); // add port number textfield

         // all set; return the JPanel
         return addressP;
      }

      /**
         Create a panel with controls to connect/disconnect to the server,
         and send data to the server.
         Connect/disconnect radio button;
         text area to enter data to send to server;
         and "send" button.

         @return a JPanel with controls to
            connect to and send data to the server
       */
      private JPanel makeHaxxorPanel() {
         // the panel to return
         final JPanel haxxorPanel = new JPanel();
         // panel for the connect button
         final JPanel connectPanel = new JPanel();
         connectPanel.add(connectB);
         // add the text area
         JPanel sendPanel = new JPanel();
         sendPanel.add(textToSend);
         // add the send button
         JPanel buttonPanel = new JPanel();
         buttonPanel.add(sendButton);
         // initialise controls to disconnected state
         actOnDisconnect();
         /*  layout:
             connect button north, text area center, send button south
          */
         haxxorPanel.setLayout(new BorderLayout());
         haxxorPanel.add(connectPanel, BorderLayout.NORTH);
         haxxorPanel.add(sendPanel, BorderLayout.CENTER);
         haxxorPanel.add(buttonPanel, BorderLayout.SOUTH);
         return haxxorPanel;
      }

      // --- Error and status messages ----------------------------------------

      /**
         Informative text to display initially in the
         {@link #status status textArea}.
       */
      private final String infoText =
         "Give the server address and port number\n"
         + "in the fields to the left;\n"
         + "use the GUI below to send requests to\n"
         + "the server, or use the `Connect' button\n"
         + "and text area to send raw data to the server.\n";

      /**
         Error message if user has not entered a server address.
       */
      private static final String NO_HOST = "Enter the server host address.";

      /**
         Error message if user has not entered the server's port number.
       */
      private static final String NO_PORT = "Enter the server port number.";

      /**
       */
      private static final String NO_CONNECTION =
         "Could not connect to server";

      /**
         Error message if I/O exception while reading data from server".
       */
      private static final String READ_ERROR =
         "Error reading data from server:\n";

      /**
         Error message if port number is not in the valid range.
       */
      private static final String PORT_NUM_RANGE =
         "Port number should be between 0 and 65535.";

      /**
       */
      private static final String CONNECTION_CLOSED_BY_SERVER =
         "Connection closed by server";

      // --- Network connection to server -------------------------------------

      /**
         Synchronization lock to ensure that
         {@link #isConnected isConnected}
         always agrees with state of connection controls.
       */
      private final Integer lock = new Integer(0);

      /**
         The current status of the persistent connection.
       */
      private volatile boolean isConnected = false;

      /**
         Socket connection to the server.
       */
      private Socket sock;
 
      /**
         Input stream to read data from the server.
       */
      private InputStream inStream;

      /**
         Output stream to send data to the server.
       */
      private OutputStream outStream;

      /**
         Thread to listen for and display data received from the server.
         An instance of this class is created when a persistent connection
         is established; as a separate thread, it listens for and displays
         data received from the server; the thread will end when the
         socket connection to the server (or its input stream) is closed.
      */
      private class ConnectionListener extends Thread {
         /**
            Read all bytes from the server and display in the
            {@link #ioPanel IOPanel}.
         */
         public void run() {
            try { // to get data from server
               int b; // store byte from server
               while ((b = inStream.read()) >= 0) { // end when inStream closed
                  // display received byte as character
                  ioPanel.appendToInput((char)b);
               }
               // end of input from server: inStream is closed
               report(CONNECTION_CLOSED_BY_SERVER);
            } catch (IOException ioe) {
               /*  thrown by inStream.read(), either because:
                   a) user has clicked Disconnect button,
                      calling sock.close() in connectB's ActionListener; or
                   b) something else went wrong, e.g., unplugged cable
                */
               if (isConnected()) { // (b) applies: inform user
                  report(READ_ERROR + ioe.getMessage());
               } // else: (a) - connection closed by user; all ok
            } catch (NullPointerException npe) {
               /*  on some JVMs, inStream may be set to null if the socket
                   connection is closed;
                   nothing useful to do except catch the exception
                */
            } finally {
               /*  for whatever reason, the socket connection is closed;
                   reset controls
                */
               actOnDisconnect();
            }
         }
      } // end class MetaClient.ConnectorPanel.ConnectionListener

      // --- Constructor ------------------------------------------------------

      /**
         Creates a new <code>ConnectorPanel</code> instance
         with given default address and port number for the server.
         ConnectorPanel has controls to specify the address of the server,
         and to maintain a persistent socket connection.
         There is also an area to display status messages.

         @param defaultAddress the default address of the server;
            if this is <code>null</code>, host will be set to
            <code>"localhost"</code>
         @param defaultPort the default port number of the server
       */
      public ConnectorPanel(final String defaultAddress,
                            final int defaultPort) {
         /*  layout:
             controls on left; status msgs on right
          */
         this.setLayout(new BorderLayout());
         // controls on left
         JPanel leftPanel = new JPanel();
         leftPanel.setLayout(new BorderLayout());
         leftPanel.add(this.makeAddressPanel(), BorderLayout.NORTH);
         leftPanel.add(this.makeHaxxorPanel(), BorderLayout.CENTER);
         this.add(leftPanel, BorderLayout.WEST);
         // status msgs on right
         this.status.append(infoText);
         this.add(this.status, BorderLayout.CENTER);
         // border to mark this out as a notional area
         this.setBorder(new EtchedBorder());

         /*  set default host address and port number:
             if host is null, set it to localhost
          */
         hostField.setText((defaultAddress == null) ?
                           "localhost" : defaultAddress);
         portField.setText("" + defaultPort);

         /*  listener for the connect button:
             toggle between connected and disconnected persistent connection.
             If connect:
             - open connection and I/O streams
             - start ConnectionListener thread
             - set controls to reflect connected state
             - if connection fails, report error and reset controls
             if disconnect:
             - close connection and reset controls to disconnected
          */
         connectB.addActionListener(
            new ActionListener() {
               public void actionPerformed(ActionEvent ae) {
                  synchronized(lock) {
                     if (connectB.isSelected()) { // connect to server
                        try { // to connect to server
                           sock = getSocket();
                           if (sock == null) {
                              /* connection failed:
                                 inform user and set controls to disconnected
                              */
                              report(NO_CONNECTION);
                              actOnDisconnect();
                              return;
                           }
                           // set up I/O streams
                           inStream = sock.getInputStream();
                           outStream = sock.getOutputStream();
                           // now connected; set controls
                           actOnConnect();
                           /*  start a thread to listen to and report
                               all data received from server
                           */
                           new ConnectionListener().start();
                        } catch (Exception e) {
                           // if anything goes wrong, reset
                           conPanel.report(NO_CONNECTION + ":\n"
                                           + e.getMessage());
                           actOnDisconnect();
                        }
                     } else { // user wants to close connection
                        closeSocket(); // close server connection
                        actOnDisconnect();
                     }
                  }
               }
            }
         );
         /* Listener for the send button.
            Get the text from the GUI and send it character-by-character
            to the server, and display the sent text.
          */
         sendButton.addActionListener(
            new ActionListener() {

               String request; // the text to send
               int len;       // its length
               char aChar;   // current character in text

               public void actionPerformed(ActionEvent ae) {
                  request = textToSend.getText();
                  len = request.length();
                  try { // to send to server
                     if (outStream != null) { // should be connected
                        for (int i = 0; i < len; i++) {
                           aChar = request.charAt(i);
                           outStream.write(aChar);         // send char
                           ioPanel.appendToOutput(aChar); // report sent char
                        }
                     }
                     // clear GUI
                     textToSend.setText("");
                     textToSend.requestFocus();
                  } catch (Exception e) {
                     // reset on failure
                     if (connectB.isSelected()) {
                        actOnDisconnect();
                     }
                  }
               }
            }
         );
      }

      // --- public methods ---------------------------------------------------

      /**
         Connect to a server at the AddressPanel address, send data,
         and get the server's reponse.
         It is assumed the server will close the connection once it has
         sent its response.
    
         @param data the string to send to the server once the connection
         is established
         @return the server's response; null if connection fails
       */
      public String handleRequest(String data) {
         String response = ""; // return value
         Socket asock = null;  // connection to server
         try { // to connect to server
            asock = this.getSocket();
            if (asock == null) {
               return null;
            }
            conPanel.reportConnection(); // report successful connection
            //set up I/O
            InputStream inStream = asock.getInputStream();
            OutputStream outStream = asock.getOutputStream();
            // send data character-by-character
            final int dataLength = data.length();
            for (int i = 0; i < dataLength; i++) {
               outStream.write(data.charAt(i));
               ioPanel.appendToOutput(data.charAt(i));
            }
            // read and display response from server
            int b = 0;
            while ((b = inStream.read()) >= 0) {
               ioPanel.appendToInput((char)b);
               response += (char)b;
            }
         } catch (UnknownHostException ioe) {
            conPanel.report("Host not recognized");
         } catch (IOException ioe) {
            conPanel.report("caught IOExc: " + ioe.getMessage());
         } finally {
            // close connection to server
            if (asock != null) {
               try {
                  asock.close();
               } catch (Exception e) {
                  conPanel.report("Error while closing socket: "
                                  + e.getMessage());
               }
            }
            conPanel.reportConnectionClosed();
         }
         return response;
      }

      /**
         Close persistent connection.
      */
      public void closeSocket() {
         if (sock != null) {
            try {
               sock.close();
            } catch (Exception e) {
               // nothing useful to do
            }
         }
      }

      // --- private methods --------------------------------------------------

      /**
         Get a socket connection to the server.
         Returns null if connection fails.

         @return a <code>Socket</code> connection to the server,
            or <code>null</code> if the connection attempt fails
         @exception UnknownHostException if the host address is not valid
         @exception IOException if an I/O error occurs
       */
      private Socket getSocket() throws UnknownHostException, IOException {
         // get the address of the server
         String addr = hostField.getText(); // host address
         String port = portField.getText(); // host port numeral
         int portNum;                       // host port number
         /* data validation:
            - check addr not empty
            - check port not empty and a number
         */
         if (addr.length() <= 0) { // no address specified by user
            this.noHost();        // inform user
            return null;         // no connection
         } else if (port.length() <= 0) { // no port specified by user
            this.noPort();               // inform user
            return null;                // no connection
         }
         // check user-specified port number is a number
         try {
            portNum = Integer.parseInt(port);
         } catch (NumberFormatException nfe) {
            this.report(PORT_NUM_RANGE);
            return null; // no connection
         }
         // check port number is in valid range
         if (portNum < 0 || portNum > 65535) {
            this.status.append(PORT_NUM_RANGE);
            return null; // no connection
         }
         // return socket connection to erver
         return new Socket(addr, portNum);
      }

      /**
         Test if the persistent connection to the server is open.

         @return true if the persistent connection to the server is open;
                 false otherwise
       */
      private boolean isConnected() {
         return isConnected;
      }

      /**
         Set controls to their disconnected state.
         This method adjusts the controls to their appropriate state
         when there is no connection to the server.
       */
      private void actOnDisconnect() {
         textToSend.setText("");
         sendButton.setEnabled(false);
         textToSend.setEditable(false);
         synchronized(lock) {
            connectB.setSelected(false);
            isConnected = true;
         }
      }

      /**
         Set controls to their connected state.
         This method adjusts the controls to their appropriate state
         when there is a connection to the server.
       */
      private void actOnConnect() {
         sendButton.setEnabled(true);
         textToSend.setEditable(true);
         textToSend.requestFocus();
         synchronized(lock) {
            connectB.setSelected(true);
            isConnected = false;
         }
      }

      /**
         Report an error message that the user has not supplied a host address
         for the server.
       */
      private void noHost() {
         this.status.append(NO_HOST);
         this.hostField.requestFocus();
      }

      /**
         Report an error message that the user has not supplied a port number
         for the server.
       */
      private void noPort() {
         this.status.append(NO_PORT);
         this.portField.requestFocus();
      }

      /**
         Report that a connection to the server has been established.
       */
      private void reportConnection() {
         this.report("Connected to " + address());
      }

      /**
         Report that the connection to the server has been closed.
       */
      private void reportConnectionClosed() {
         this.report("Connection to " + address()
                            + " has been closed.");
      }

      /**
         Display a status message.

         @param line the message to display
       */
      private void report(final String line) {
         this.status.append(line + "\n");
      }

      /**
         The address of the server in <code>host:port</code> format.

         @return the address of the server in <code>host:port</code> format
       */
      private String address() {
         return this.hostField.getText() + ":" + this.portField.getText();
      }

   } // end inner class ConnectorPanel


   /**
      Panel showing input/output from/to the server.
    */
   private static class IOPanel extends JPanel {

      /**
         Display to show input received from the server.
      */
      private final DisplayPanel input;

      /**
         Display to show output sent to the server.
       */
      private final DisplayPanel output;


      /**
         Create an IOPanel.
         This has two {@link DisplayPanel DisplayPanels}
         showing input from the server and output to the server.
       */
      public IOPanel() {
         this.input = new DisplayPanel();
         this.output = new DisplayPanel();
         // panel with titled border for input
         JPanel ip = new JPanel();
         ip.add(this.input);
         ip.setBorder(new TitledBorder("From the server"));
         // panel with titled border for output
         JPanel op = new JPanel();
         op.add(this.output);
         op.setBorder(new TitledBorder("To the server"));
         // put DisplayPanels one above the other
         this.setLayout(new GridLayout(2,1));
         this.add(op);
         this.add(ip);
      }


      /**
         Append a character to the output display.

         @param c the character to append
       */
      public void appendToOutput(final char c) {
         this.output.appendChar(c);
      }

      /**
         Append a character to the input display.

         @param c the character to append
      */
      public void appendToInput(final char c) {
         this.input.appendChar(c);
      }
   } // end inner class IOPanel


   /**
      The {@link ConnectorPanel ConnectorPanel} instance.
      This allows the user to specify the server address,
      and to connect to and send data to the server.
   */
   private final ConnectorPanel conPanel;

   /**
      Display to show data sent to and received from the server.
   */
   private final IOPanel ioPanel = new IOPanel();

   /**
    * Describe variable <code>guiBoss</code> here.
    *
    */
   //   private final GUIControl guiBoss;

   /**
      The client GUI to display and monitor.
   */
   private final ProxyableComponent clientGUI;

   // --- Constructor ---------------------------------------------------------

   /**
    * Creates a new <code>MetaClient</code> instance.
    *
    * @param gui a <code>ConnectableComponent</code> value
    */
   public MetaClient(final ProxyableComponent gui) {
      clientGUI = gui;
      clientGUI.setProxy(this);
      //      guiBoss = new GUIControl();
      conPanel = new ConnectorPanel(gui.getDefaultHost(),
                                    gui.getDefaultPort());
      this.add(conPanel, BorderLayout.NORTH);
      this.add(ioPanel, BorderLayout.WEST);
      this.add(gui.getGUI(), BorderLayout.CENTER);

      this.pack();

      this.addWindowListener(this);
   }

   // --- Implementation of Proxy ---------------------------------------------

   /**
      Connect to the server, send data,
      and return the server's reponse.
      The given string should be a complete request;
      the returned string should be the server's response,
      and it is assumed that the server closes the connection
      after the response has been sent.
    
      @param data the string to send to the server    
      @return the server's response
   */
   public String handleRequest(String data) {
      return conPanel.handleRequest(data);
   }
   // --- end implementation of Proxy

   // --- Implementation of java.awt.event.WindowListener ---------------------

   /**
      Do nothing.
      @param windowEvent a <code>WindowEvent</code> value
    */
   public final void windowActivated(final WindowEvent windowEvent) {

   }

   /**
      Do nothing
      @param windowEvent a <code>WindowEvent</code> value
    */
   public final void windowClosed(final WindowEvent windowEvent) {

   }

   /**
    * Describe <code>windowClosing</code> method here.
    *
    * @param windowEvent a <code>WindowEvent</code> value
    */
   public final void windowClosing(final WindowEvent windowEvent) {
      // just to be nice, try to close any open connections
      conPanel.closeSocket();
      // and shut down JVM
      System.exit(0);
   }

   /**
      Do nothing
      @param windowEvent a <code>WindowEvent</code> value
    */
   public final void windowDeactivated(final WindowEvent windowEvent) {

   }

   /**
      Do nothing
      @param windowEvent a <code>WindowEvent</code> value
    */
   public final void windowDeiconified(final WindowEvent windowEvent) {

   }

   /**
      Do nothing
      @param windowEvent a <code>WindowEvent</code> value
    */
   public final void windowIconified(final WindowEvent windowEvent) {

   }

   /**
      Do nothing
      @param windowEvent a <code>WindowEvent</code> value
    */
   public final void windowOpened(final WindowEvent windowEvent) {

   }

   // --- end implementation of java.awt.WindowListener

   /**
    * Describe <code>main</code> method here.
    *
    * @param args a <code>String</code> value
    */
   public static void main(String[] args) {
      Comp213.FriendFaceClient.FriendFaceGUI ffg =
            new Comp213.FriendFaceClient.FriendFaceGUI();
      MetaClient mc = new MetaClient(ffg);
      mc.setVisible(true);
   }
}
