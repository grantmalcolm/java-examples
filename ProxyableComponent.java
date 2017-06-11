/*
 * ~/JavaClasses/Comp213/MetaClient/ProxyableComponent.java
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

import javax.swing.JComponent;

/**
   A client with a GUI that can delegate interactions with its server
   to a {@link Proxy Proxy}.
   Such clients can be displayed in {@link MetaClient MetaClient},
   and requests to the server can be sent through MetaClient.
   Classes implementing this interface must provide a default address
   and port number for the server that they use.

   @author <a href="mailto:grant@liverpool.ac.uk">Grant Malcolm</a>
   @version 1.0
 */
public interface ProxyableComponent extends Client {

   /**
      Give a {@link javax.swing.JComponent JComponent} to display
      in {@link MetaClient MetaClient}.
      
      @return the GUI for the client
    */
   public JComponent getGUI();

   /**
      Delegate interactions with the server to the given
      {@link Proxy Proxy}.
      
      @param proxy the <code>Proxy</code> to which interactions with
         the server are to be delegated
    */
   public void setProxy(Proxy proxy);

}
