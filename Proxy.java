/*
    ~/JavaClasses/Comp213/MetaClient/Proxy.java
 
    Created: Tue Nov 26 22:30:30 2013

 
    copyright Grant Malcolm
 
    This source code may be freely used, modified, or distributed
    provided due credit is given.

 */

package Comp213.MetaClient;


/**
   Interface for classes that can act as proxy or intermediary for a server.
   Requests to the server go through the proxy:
   it is assumed that implementing classes make a connection to the server,
   then send the given string, which serves as a complete request;
   the server then sends a response (the returned string) and closes the
   connection.

   @author <a href="mailto:grant@liverpool.ac.uk">Grant Malcolm</a>
   @version 1.0
 */
public interface Proxy {

   /**
      Connect to a server, send data,
      and return the server's reponse.
      The given string should be a complete request;
      the returned string should be the server's response,
      and it is assumed that the server closes the connection
      after the response has been sent.
    
      @param data the string to send to the server    
      @return the server's response
   */
   public String handleRequest(String data);
}
