/*
 * /home/grant/JavaClasses/Comp213/LikeTunes/LikeTunesServer.java
 *
 * Created: Thu Oct 16 22:42:14 2014
 *
 * copyright Grant Malcolm
 *
 *   This source code is part of a model solution for an assignment
 *   for COMP213 at the University of Liverpool.
 *   Please do not archive, distribute, or make this code publicly available.
 *
 */

package Comp213.LikeTunes;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *   LikeTunes server for COMP213 assignment.
 *   Sets up a server socket on port 12000 by default;
 *   specify a port number by using <code>-port nnnnn</code>
 *   as a command-line option.
 *   Shut the server down by connecting to the port
 *   and entering line <code>.houEensOp</code>.
 *   </p><p>
 *   Exit codes:
 *   <ol>
 *    <li> everything cool; </li>
 *    <li> I/O error; </li>
 *    <li> command-line syntax error. </li>
 *   </ol>
 *   </p>
 *
 *   @author <a href="mailto:grant@liverpool.ac.uk">Grant Malcolm</a>
 *   @version 1.0
 */
public class LikeTunesServer {

   /**
    *   Constants used in the LikeTunes server protocol.
    */
   public static interface Protocol {
      
      /**
       *   Code to request adding a tune to the server.
       *   After connecting to the server, the client sends a line beginning
       *   with this character to indicate they wish to add a tune to the
       *   server.  The next line should be the name of the artist; the line
       *   after that should be the title of the song.  The server then adds
       *   that tune and closes the connection.
       */
      public static final char ADD_REQ = '0';

      /**
       *   Code to request liking a tune on the server.
       *   After connecting to the server, the client sends a line beginning
       *   with this character to indicate they wish to like a tune on the
       *   server.  The next line should be the name of the artist; the line
       *   after that should be the title of the song.  The server then likes
       *   that tune and closes the connection.
       */
      public static final char LIKE_REQ = '1';

      /**
       *   Code to request the list of tunes in alphabetical order.
       *   After connecting to the server, the client sends a line beginning
       *   with this character to indicate the wish to view all tunes on the
       *   server in alphabetical order.  The server then sends all tunes in
       *   alphabetical order, with artist, title, and number of likes on
       *   separate lines, then closes the connection.
       */
      public static final char ALPHLIST_REQ = '2';

      /**
       *   Code to request the list of tunes in order of popularity.
       *   After connecting to the server, the client sends a line beginning
       *   with this character to indicate the wish to view all tunes on the
       *   server in order of popularity.  The server then sends all tunes in
       *   popularity order, with artist, title, and number of likes on
       *   separate lines, then closes the connection.
       */
      public static final char POPLIST_REQ = '3';

      /**
       *   Code to indicate that the client connection was closed prematurely.
       *   When the client requests to {@link #ADD_REQ add} or
       *   {@link #LIKE_REQ like} a tune, the client sends the artist and title
       *   on subsequent lines; if the client connection is closed before
       *   both these lines are read, the server responds by sending a line
       *   consisting of this string.
       *   The server then closes the connection.
       */
      public static final char CC_ERR = '4';

      /**
       *   Code to indicate that the client has sent an empty line of text.
       *   All data sent between client and server should be non-empty;
       *   if the server receives an empty line of data from a client,
       *   the server responds by sending a line consisting of this string.
       *   The server then closes the connection.
       */
      public static final char CD_ERR = '5';

      /**
       *   Code to indicate a fatal I/O error.
       *   If an I/O error occurs that prevents the server correctly
       *   responding to a client's request, the server responds by sending
       *   a line consisting of this string.  The server then closes the
       *   connection.
       */
      public static final char IO_ERR = '6';

      /**
       *   Code to indicate the client has sent an unrecognised request.
       *   If the first line sent by the client does not begin with a
       *   character to request {@link #ADD_REQ adding} or
       *   {@link #LIKE_REQ liking} a tune, or to view tunes by
       *   {@link #ALPHLIST_REQ alphabetical} or
       *   {@link #POPLIST_REQ popularity} order,
       *   the server responds by sending a line consisting of this string.
       *   The server then closes the connection.
       */
      public static final char CS_ERR = '7';

      /**
       *   Code to be entered by remote user to shut down the server.
       */
      public static final String QUITC = ".houEensOp";

   }// end inner class Protocol


   /**
    *   Exception thrown when server receives an empty line of text.
    *
    *   @see Protocol#CD_ERR
    */
   private static class ClientDataException extends Exception {
   }

   /**
    *   Exception thrown when client connection is prematurely closed.
    *
    *   @see Protocol#CC_ERR
    */
   private static class ClientClosureException extends Exception {
   }

   /**
    *   Exception thrown when client sends an unrecognised request-code.
    *
    *   @see Protocol#CS_ERR
    */
   private static class ClientSyntaxException extends Exception {
   }


   /**
    *   Class to handle a client session in a separate thread.
    */
   private static class SessionHandler 
      implements Runnable, Protocol {

      /**
       *   The socket connection to the client for this session.
       */
      private final Socket client;

      /**
       *   The input stream from this client.
       */
      private BufferedReader in;


      /**
       *   Creates a new <code>SessionHandler</code> instance.
       *
       *   @param c the client Socket for this session
       */
      SessionHandler(final Socket c) {
         this.client = c;
      }


      /**
       *   Handle one request from a client.
       *   Read the request from the client, then service it following the
       *   {@link Protocol LikeTunes server protocol}.
       */
      public void run() {
         // the output stream to the client
         PrintWriter out = null;

         try { // to set up I/O and service client's request
            out = new PrintWriter(
                     new OutputStreamWriter(client.getOutputStream()));
            in = new BufferedReader(
                     new InputStreamReader(client.getInputStream()));

            // get the client's request
            String clientInput = getDataLine();  // line of input from client
            char clientReq = clientInput.charAt(0);  // client command
            /*  if client wants to add or like a tune, they need to give
             *  artist and title of that tune; these variables store that data
             *  if needed
             */
            String artist;
            String title;
            //  now determine what the client wants, and do it
            if (clientReq == ALPHLIST_REQ) { // tunes in alphabetical order
               // send tunes in alphabetical order
               out.print(TUNES.listAlphabetically());
            } else if (clientReq == POPLIST_REQ) { // tunes in pop order
               // send tunes in popularuty order
               out.print(TUNES.listByLikes());
            } else if (clientReq == ADD_REQ) { // add a tune
               // get client data
               artist = getDataLine();
               title = getDataLine();
               // add the tune
               TUNES.addTune(artist, title);
            } else if (clientReq == LIKE_REQ) { // like a tune
               // get client data
               artist = getDataLine();
               title = getDataLine();
               // like that tune
               TUNES.likeTune(artist, title);
            } else if (clientInput.equals(QUITC)) {
               // shut down server
               LikeTunesServer.shutdown();
            } else {
               // client request not a recognised code
               throw new ClientSyntaxException();
            }
         } catch (IOException ioe) {
            // I/O error prevents fulfilling request
            out.println(IO_ERR);
         } catch (ClientDataException ioe) {
            // empty line received
            out.println(CD_ERR);
         } catch (ClientClosureException ioe) {
            // client connection closed before tune data received
            out.println(CC_ERR);
         } catch (ClientSyntaxException ioe) {
            // client request not a recognised code
            out.println(CS_ERR);
         } finally {
            // session done: send output and close resources
            out.flush(); // make sure data is sent
            // close down
            try { // to close input stream
               in.close();
            } catch (IOException ioe) {
               // nothing useful to do
            }
            // close output stream
            if (out != null) {
               out.close();
            }
            // close client connection
            if (client != null) {
               try { // to close client socket
                  client.close();
               } catch (IOException ioe) {
                  // nothing useful to do
               }
            }
         }// end try-catch-finally
      }// end run method

      /**
       *   Read a line of data from client.  All lines should be non-empty.
       *   This method is only called when a non-empty line of data is
       *   expected from the client
       *
       *   @return the non-empty string received from the client
       *
       *   @exception ClientDataException if client send empty string
       *   @exception ClientClosureException if client closes connectio
       *   @exception IOException if an I/O error occurs
       */
      private String getDataLine()
      throws ClientDataException, ClientClosureException, IOException {
         String data = in.readLine(); // client data
         if (data == null) {
            // client has closed connection
            throw new ClientClosureException();
         }
         if (data.length() <= 0) {
            // client has sent empty line
            throw new ClientDataException();
         }
         // else: connection has not been closed and data is non-empty
         return data;
      }
   }  // end inner class SessionHandler


   /**
    *   Port number for the server socket. Default is 12000.
    */
   private static int portNum = 12000;

   /**
    *   The server socket.
    */
   public static ServerSocket theSocket;

   /**
    *   Indicates that the server has received a request to shut down.
    */
   private static boolean shutdownReq = false;

   /**
    *   The list of tunes on the server.
    */
   private static final TuneList TUNES = new TuneList();


   /**
    *   Used to shut the server down.
    */
   public static void shutdown() {
      // flag that shutdown has been requested
      shutdownReq = true;
      System.out.println("Shutting the LikeTunes server down");

      try {
         /*  close the server socket;
          *  the accept()-loop in main will throw
          *  a SocketException
          */
         theSocket.close();
      }
      catch (IOException ioe) {
         if (theSocket != null && !theSocket.isClosed()) {
            // something's gone badly wrong
            System.err.println("Can't close the Server Socket:");
            System.err.println(ioe.getMessage());
            // just have to trust the interpreter to shut down nicely
            System.exit(1);
         }
      }
   }


   /**
    *   Set up the LikeTunes server
    *
    *   @param args command-line options:
    *    <ul>
    *     <li> <code>-port nnnnn</code> - run server on port number nnnnn;
    *       default is 12000
    *    </ul>
    */
   public static void main(String[] args)
   {
      // check if port number is specified
      if (args.length > 0) {
         if (args.length == 2 && args[0].equals("-port")) {
            try {
               // 2nd argument should be the port number
               portNum = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException nfe) {
               // bad command-line arguments
               System.err.println("invalid port number");
               System.exit(2);
            }
         }
         else
         {
            // only accept "-port nnnnn" as valid command-line arguments
            System.err.println("usage: -port n");
            System.exit(2);
         }
      }

      // set up the list of tunes
      TuneList tunes = new TuneList();

      // set up server socket
      try {
         theSocket = new ServerSocket(portNum);
      }
      catch (IOException ioe) {
         // fatal error; print info...
         System.err.println("LikeTunes: could not create server socket");
         System.err.println(ioe.getMessage());
         System.exit(1);
      }
      System.out.println("LikeTunes server has started on port " + portNum);

      // Thread-pool for session-handlers
      ExecutorService threadPool = Executors.newCachedThreadPool();

      /*  Start loop to accept incoming connections.
       *  For each incoming socket connection, start up a session-handler.
       */
      Socket incoming = null;     // new connection.

      boolean incomingOK = true;  // is connection established?
                                 //  Keep this true by default;
                                //   set to false on error.

      while (true) { // keep accepting requests
         try {
            // to get next client
            incoming = theSocket.accept( );
         }
         catch (IOException ioe) {
            // Is error fatal?
            if (theSocket == null || theSocket.isClosed()) {
               // no server socket: shut the server down

               // first close the queue of executors
               threadPool.shutdown();

               //  wait for any recent connections to be processed;
               try {
                  Thread.sleep(2000);
               }
               catch (InterruptedException ie) {
                  // shouldn't happen
               }

               if (shutdownReq) { // server has been asked to shut down
                  if (Thread.activeCount() > 1) {
                     System.err.println("LikeTunes: "
                                        + "some threads have not ended...");
                     System.err.println("...exiting java interpreter");
                     System.exit(1);
                  }
                  // else: just main thread left, so end it
                  return;
               }
               /*  
                *  If we're here, then shutdown has not been requested
                *  and server socket is dead;
                *  so nothing to do but complain and leave
                */
               System.err.println("LikeTunes: fatal error -");
               System.err.println("   " + ioe.getMessage());
               System.exit(1);
            }
            /*
             *  otherwise: server socket is still open,
             *  just the incoming socket died;
             *  flag this so we can ignore the last connection
             */
            incomingOK = false;
         } // end catch-block for I/O errors

         // back to the latest connection
         if (incomingOK) {
            // start new thread to serve new client
            threadPool.execute(new SessionHandler(incoming));
         } else {
            /*  there was an IOException, but server socket is still open:
             *  get back to main while-loop and set incomingOK to default
             */
            incomingOK = true;
         }
      }  // end while loop
   }
}
