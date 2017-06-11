/*
 * /home/grant/JavaClasses/Comp213/FriendFaceClient/FriendFaceGUI.java
 *
 * Created: Fri Oct 23 23:53:53 2015
 *
 * copyright Grant Malcolm
 *
 *   This source code may be freely used, modified, or distributed
 *   provided due credit is given.
 *
 * TO DO:
 * - documentation
 * - default proxy implementation
 *
 */

package Comp213.FriendFaceClient;

import javax.swing.JPanel;
import Comp213.MetaClient.Proxy;
import Comp213.MetaClient.DisplayPanel;
import Comp213.MetaClient.ProxyableComponent;
import Comp213.FriendFace.FriendFaceProtocol;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JButton;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.border.EtchedBorder;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import java.net.UnknownHostException;
import java.io.IOException;
import Comp213.MetaClient.MetaClient;
import javax.swing.JComponent;


/**
 * Describe class FriendFaceGUI here.
 *
 *
 * @author <a href="mailto:grant@liverpool.ac.uk">Grant Malcolm</a>
 * @version 1.0
 */
public class FriendFaceGUI extends JComponent implements ProxyableComponent {

   public JComponent getGUI() {
      return this;
   }

   /**
    * Describe variable <code>proxy</code> here.
    *
    */
   protected Proxy proxy;

   /**
    * Describe <code>setProxy</code> method here.
    *
    * @param proxy a <code>Proxy</code> value
    */
   public void setProxy(Proxy proxy) {
      this.proxy = proxy;
   }

   public String getDefaultHost() {
      return "localhost";
   }

   public int getDefaultPort() {
      return 12000;
   }

   private final DisplayPanel display;

   private final JTextField memberField = new JTextField(20);

   private final JTextField requesterField = new JTextField(20);

   private final JTextField soughtField = new JTextField(20);

   /**
    * Creates a new <code>FriendFaceGUI</code> instance.
    *
    */
   public FriendFaceGUI() {

      display = new DisplayPanel(12, 33, false);

      JPanel registerPanel = new JPanel();
      JPanel memPanel = new JPanel();
      memPanel.setLayout(new FlowLayout());
      memPanel.add(new JLabel("Member name:"));
      memPanel.add(memberField);
      JPanel regButtonPanel = new JPanel();
      JButton regB = new JButton("Register");
      JButton reqB = new JButton("Get Requests");
      JButton ffriendB = new JButton("Get Friends' Friends");
      regButtonPanel.setLayout(new FlowLayout());
      regButtonPanel.add(regB);
      regButtonPanel.add(reqB);
      regButtonPanel.add(ffriendB);
      registerPanel.setLayout(new BorderLayout());
      registerPanel.add(memPanel, BorderLayout.CENTER);
      registerPanel.add(regButtonPanel, BorderLayout.SOUTH);
      registerPanel.setBorder(new EtchedBorder());

      JPanel friendPanel = new JPanel();
      JPanel fieldPanel = new JPanel();
      fieldPanel.setLayout(new GridBagLayout());

      // Constraints for the components
      GridBagConstraints grid = new GridBagConstraints();

      // not sure about this
      grid.ipadx = 2;
      grid.ipady = 2;
      // grid.insets = new Insets(10, 5, 5, 5);


      /* ---------------------------------------------------------------------
       *  Row 1
       *
       */

      // label for remote server control
      JLabel requesterLabel = new JLabel("Requesting member:");
      grid.gridx = 0;
      grid.gridy = 0;
      grid.fill = GridBagConstraints.HORIZONTAL;
      fieldPanel.add(requesterLabel, grid);

      grid.gridx = 1;
      grid.gridy = 0;
      grid.fill = GridBagConstraints.HORIZONTAL;
      fieldPanel.add(requesterField, grid);


      /* ---------------------------------------------------------------------
       *  Row 2
       *
       */

      // label for control to specify port number
      JLabel soughtLabel = new JLabel("Sought member:");
      grid.gridx = 0;
      grid.gridy = 1;
      grid.fill = GridBagConstraints.HORIZONTAL;
      fieldPanel.add(soughtLabel, grid);

      grid.gridx = 1;
      grid.gridy = 1;
      grid.fill = GridBagConstraints.HORIZONTAL;
      fieldPanel.add(soughtField, grid);

      JPanel friendButtonPanel = new JPanel();
      friendButtonPanel.setLayout(new FlowLayout());
      JButton addReqB = new JButton("Add Request");
      JButton accReqB = new JButton("Accept Request");
      JButton refReqB = new JButton("Refuse Request");
      friendButtonPanel.add(addReqB);
      friendButtonPanel.add(accReqB);
      friendButtonPanel.add(refReqB);
      friendPanel.setLayout(new BorderLayout());
      friendPanel.add(fieldPanel, BorderLayout.CENTER);
      friendPanel.add(friendButtonPanel, BorderLayout.SOUTH);
      friendPanel.setBorder(new EtchedBorder());

      this.setLayout(new BorderLayout());
      this.add(registerPanel, BorderLayout.NORTH);
      this.add(friendPanel, BorderLayout.CENTER);
      this.add(display, BorderLayout.SOUTH);

      regB.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ae) {
               register();
            }
         }
      );

      reqB.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ae) {
               getReqs();
            }
         }
      );

      ffriendB.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ae) {
               getFFs();
            }
         }
      );

      addReqB.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ae) {
               addReq();
            }
         }
      );

      accReqB.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ae) {
               accept();
            }
         }
      );

      refReqB.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ae) {
               refuse();
            }
         }
      );
   }

   private void register() {
      String memberName = memberField.getText();
      String response =
         proxy.handleRequest(FriendFaceProtocol.register(memberName));
      // TO DO: check not null
      if (response.equals("")) {
         display.append("Error: got empty string from server.\n");
      } else if (response.charAt(0) == FriendFaceProtocol.OK) {
         display.append(memberName + " registered.\n");
      } else if (response.charAt(0) == FriendFaceProtocol.ERR) {
         display.append(memberName + " already taken in FriendFace.\n");
      } else {
         display.append("Error: unrecognized response from server.\n");
      }
   }

   public void getReqs() {
      String memberName = memberField.getText();
      String response =
         proxy.handleRequest(FriendFaceProtocol.getRequests(memberName));
      String[] names = response.split("\n");
      for (int i = 1; i < names.length; i++) {
         display.append(names[i] + "\n");
      }
      String plural = (names.length > 1) ? "" : "s";
      display.append(names.length - 1 + " request" + plural + "\n");
   }

   private void getFFs() {
      String memberName = memberField.getText();
      String response =
         proxy.handleRequest(
            FriendFaceProtocol.ffriends(memberName)
         );
      String[] names = response.split("\n");
      for (int i = 1; i < names.length; i++) {
         display.append(names[i] + "\n");
      }
      String plural = (names.length > 1) ? "" : "s";
      display.append(names.length - 1 + " friend" + plural + " of friends.\n");
   }

   public void addReq() {
      String requesterName = requesterField.getText();
      String soughtName = soughtField.getText();
      String response =
         proxy.handleRequest(
            FriendFaceProtocol.request(requesterName, soughtName)
         );
      String req = "Request [" + requesterName + ", " + soughtName + "] ";
      if (response.equals("")) {
         display.append("Error: got empty string from server.\n");
      } else if (response.charAt(0) == FriendFaceProtocol.OK) {
         display.append(req + "added.\n");
      } else if (response.charAt(0) == FriendFaceProtocol.ERR) {
         display.append(req + "not added.\n");
      } else {
         display.append("Error: unrecognized response from server.\n");
      }
   }

   public void accept() {
      String requesterName = requesterField.getText();
      String soughtName = soughtField.getText();
      String response =
         proxy.handleRequest(
            FriendFaceProtocol.acceptRequest(requesterName, soughtName)
         );
      String req = "Request [" + requesterName + ", " + soughtName + "] ";
      if (response.equals("")) {
         display.append("Error: got empty string from server.\n");
      } else if (response.charAt(0) == FriendFaceProtocol.OK) {
         display.append(req + "accepted.\n");
      } else if (response.charAt(0) == FriendFaceProtocol.ERR) {
         display.append(req + "does not exist.\n");
      } else {
         display.append("Error: unrecognized response from server.\n");
      }
   }

   public void refuse() {
      String requesterName = requesterField.getText();
      String soughtName = soughtField.getText();
      String response =
         proxy.handleRequest(
            FriendFaceProtocol.refuseRequest(requesterName, soughtName)
         );
      String req = "Request [" + requesterName + ", " + soughtName + "] ";
      if (response.equals("")) {
         display.append("Error: got empty string from server.\n");
      } else if (response.charAt(0) == FriendFaceProtocol.OK) {
         display.append(req + "refused.\n");
      } else if (response.charAt(0) == FriendFaceProtocol.ERR) {
         display.append(req + "does not exist.\n");
      } else {
         display.append("Error: unrecognized response from server.\n");
      }
   }

   public static void main(String[] args) {

      JFrame frame = new JFrame("Testing FriendFaceGUI.java");
      frame.add(new FriendFaceGUI(), BorderLayout.CENTER);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      frame.setVisible(true);
   }
}
