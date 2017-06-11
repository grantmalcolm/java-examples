/*
 * copyright Grant Malcolm
 *
 *   This is a model solution for a practical assignment for
 *   COMP213, Advanced Object-oriented Programming, at the
 *   University of Liverpool.  Please do not distribute or
 *   archive this source code.
 *
 */

package Comp213.FriendFace;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
   FriendFace server.

   @author <a href="mailto:grant@liverpool.ac.uk">Grant Malcolm</a>
   @version 1.0
 */
public class FriendFaceServer {

   /**
      Port number for the server socket. Default is 12000.
    */
   private static int portNum = 12000;

   /**
      The server socket.
    */
   private static ServerSocket theSocket;

   /**
      Indicates that the server has received a request to shut down.
    */
   private static boolean closeReq = false;

   /**
      The list of active session-handlers.
      When a new connection is received,
      a {@link FFSessionHandler session-handler} is created to
      {@link FFSessionHandler#run() serve} the new client.
      When a session ends, the handler removes itself from the list;
      all accesses to the list should be
      {@link #connectionsLock synchronized} to prevent interference.
    */
   private static FFSessionHandler activeConnections;

   /**
      Lock to control concurrent accesses to
      {@link #activeConnections list of active session-handlers}.
    */
   private static final Integer connectionsLock = new Integer(0);

   /**
      Used to shut the server down.
      Closes the {@link #theSocket server socket}.
    */
   public static void close() {
      // flag that shutdown has been requested
      closeReq = true;
      System.out.println("Shutting the FriendFace server down");
      try {
         /* close the server socket;
          * the accept()-loop in main will throw
          * a SocketException
          */
         theSocket.close();
      } catch (IOException ioe) {
         if (theSocket != null && ! theSocket.isClosed()) {
            // unlikely, but something's gone badly wrong
            System.err.println("Exception thrown while closing:");
            System.err.println(ioe.getMessage());
            System.err.println("Trusting the interpreter to shut down nicely");
            System.exit(1);
         }
      }
   }

   /**
      Set up the FriendFace server on a given port number.
      Usage: "<code>java FriendFaceServer -port n</code>";
      if no commandline agruments are given,
      the port number defaults to 12000.

      <p>Exit codes:
      <ul>
      <li><code>0</code> Server exits normally</li>
      <li><code>1</code> IOException thrown by server socket</li>
      <li><code>2</code> Syntax error in command-line arguments</li>
      </ul>
      </p>

      @param args command line arguments:
                  "<code>-port n</code>" to set up the server on port number n
    */
   public static void main(String[] args) {
      // check if port number is specified
      if (args.length > 0) {
         if (args.length == 2 && args[0].equals("-port")) {
            try {
               // 2nd argument should be the port number
               portNum = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
               // bad command-line arguments
               System.err.println("invalid port number");
               System.exit(2);
            }
         } else {
            // only accept "-port n" as valid command-line arguments
            System.err.println("usage: -port n");
            System.exit(2);
         }
      }

      // the FriendFace dB
      final FriendFace fFace = new FriendFace();

      // set up server socket
      try  { // set up server
         theSocket = new ServerSocket(portNum);
      } catch (IOException ioe) {
         // fatal error; print info...
         System.err.println("FriendFace Server: "
                            + "could not create server socket");
         System.err.println(ioe.getMessage());
         System.exit(1);
      }
      System.out.println("FriendFace Server has been started on port "
                         + portNum);

      // Thread-pool for session-handlers
      final ExecutorService threadPool = Executors.newCachedThreadPool();

      /* Start loop to accept incoming connections.
       * For each incoming connection, start up a session-handling thread
       */
      Socket incoming = null;     // new connection.
      FFSessionHandler handler;  // handler for new connection

      try {
         while (true) { // keep accepting requests
            // get next client
            incoming = theSocket.accept( );
            /* create a new handler for this client;
               add to list of active connections; and
               add the handler to the list of tasks to be executed
             */
            handler = new FFSessionHandler(incoming, fFace);
            // add handler to list of active connections
            synchronized (connectionsLock) {
               handler.next = activeConnections;
               if (activeConnections != null) {
                  activeConnections.prev = handler;
               }
               activeConnections = handler;
            }
            // add thread to tasks
            threadPool.execute(handler);
         }
      } catch (IOException ioe) {
         /* No server socket: shut the server down.
            This might happen if an IOException occurs due to a
            connectivity failure, or if close() is called;
            in either case, try to close all active connections
          */
         // don't accept any new connections
         threadPool.shutdown();
         // wait for any recent connections to be processed
         if (Thread.activeCount() >  1) {
            System.out.print("FriendFace server: "
                             + "waiting for queued requests to complete...");
            try { // wait for up to 5 seconds
               threadPool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
               // shouldn't happen
            }
         }
         /* If there are still active threads after waiting 5 seconds,
            try closing all connections
          */
         if (Thread.activeCount() > 1) {
            // there are still some active session-handlers; shut them down
            System.out.print("FriendFace Server: ");
            System.out.println("killing recalcitrant clients");
            Socket socket; // handler's socket
            synchronized (connectionsLock) {
               FFSessionHandler tmp = activeConnections;
               while (tmp != null) {
                  socket = tmp.socket;
                  if (socket != null && ! socket.isClosed()) {
                     try {
                        socket.close();
                     } catch (Exception e) {
                        System.err.println("Exception: " + e.getMessage());
                     }
                  }
                  tmp = tmp.next;
               }
            }
         }
         if (! closeReq) {
            // not everything is as it should be
            System.err.println("FriendFace server: fatal error -");
            System.err.println("   "+ioe.getMessage());
            System.exit(1);
         }
      }
   }

   /**
      Handle one FriendFace session in a Thread.
      Any exceptions thrown should just end the session.
      All handlers are stored in a {@link #activeConnections linked list}.
    */
   private static class FFSessionHandler
      implements Runnable {

      /**
         The thread-safe list of members.
       */
      private final FriendFace fFace;

      /**
         Input stream from remote client.
       */
      private InputStream in;

      /**
         Output stream to remote client.
       */
      private PrintWriter out;

      /**
         The socket connection with the remote client.
       */
      private final Socket socket;

      /**
         The next handler in the {@link #activeConnections linked list}.
       */
      private FFSessionHandler next;

      /**
         The preceding handler in the {@link #activeConnections linked list}.
       */
      private FFSessionHandler prev;


      /**
       * Creates a new <code>FFSessionHandler</code> instance.

       * @param s the socket connection to the remote client
       * @param ff a <code>FriendFace</code> value
       */
      public FFSessionHandler(Socket s, FriendFace ff) {
         fFace = ff;
         socket = s;
      }

      /**
         The session with the remote client, running in a separate thread.
         Read the request from the client, and respond according to the
         protocol specified in
         <a href="http://www.csc.liv.ac.uk/~grant/Teaching/COMP213/Assignments/FriendFace/Ass2/friendFaceServer.maude">friendFaceServer.maude</a>.
       */
      public void run() {
         String line; // to store the input request from remote client
         try { // to set up I/O; get request & respond
            in = socket.getInputStream();
            out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream()));

            // get request from client
            line = this.readData();

            if (line.equals(FriendFaceProtocol.QUITC)) {
               close();
               return;
            }
            // to store client input and server response
            String name1, name2, result;
            // find out what the client wants
            char prefix = line.charAt(0);
            switch (prefix) {
               case FriendFaceProtocol.REGISTER:
                  // register a new member
                  name1 = readData();
                  char resp = fFace.register(name1) ?
                              FriendFaceProtocol.OK :
                              FriendFaceProtocol.ERR;
                  out.println(resp);
                  break;
               case FriendFaceProtocol.GETFREQS:
                  // get friendship requests for a member
                  name1 = readData();
                  result = fFace.getRequests(name1);
                  out.println(FriendFaceProtocol.OK);
                  out.print(result + (result.equals("") ? "\n" : ""));
                  break;
               case FriendFaceProtocol.ADDREQ:
                  // add a friendship request
                  name1 = readData();
                  name2 = readData();
                  out.println(fFace.addRequest(name1, name2) ?
                              FriendFaceProtocol.OK : FriendFaceProtocol.ERR);
                  break;
               case FriendFaceProtocol.ACCEPT:
                  // accept a friendship request
                  name1 = readData();
                  name2 = readData();
                  out.println(fFace.acceptRequest(name1, name2) ?
                              FriendFaceProtocol.OK : FriendFaceProtocol.ERR);
                  break;
               case FriendFaceProtocol.REFUSE:
                  // refuse a friendship request
                  name1 = readData();
                  name2 = readData();
                  out.println(fFace.refuseRequest(name1, name2) ?
                              FriendFaceProtocol.OK : FriendFaceProtocol.ERR);
                  break;
               case FriendFaceProtocol.GETFF:
                  // get friends of friends
                  name1 = readData();
                  result = fFace.getFFriends(name1);
                  out.println(FriendFaceProtocol.OK);
                  out.print(result + (result.equals("") ? "\n" : ""));
                  break;
               default: // bad client input: end session
                  out.println(FriendFaceProtocol.PROTOCOL_ERR);
                  return;
            }
            out.flush();
         } catch (IOException ioe) {
            // faulty connection to client; nothing useful to do
         } catch (ClientDataException cde) {
            // bad protocol from client; unexpected input
            out.println(FriendFaceProtocol.PROTOCOL_ERR);
            out.flush();
         } finally {
            // clean up
            if (socket != null) {
               try {
                  out.close();
                  socket.close();
               } catch (IOException e) { }
            }
            // remove from list
            synchronized (connectionsLock) {
               // adjust next pointers
               if (prev == null) { // we're first in list
                  activeConnections = next;
               } else { // we're not first in list
                  prev.next = next;
               }
               // adjust prev pointers
               if (next != null) {
                  next.prev = prev;
               }
            }
         }
         // end of session
      }

      /**
         Read a non-empty line of client input.

         @return the non-empty line from client input
         @exception IOException if an I/O error occurs
         @exception ClientDataException if the line is empty
       */
      private String readData() throws IOException, ClientDataException {
         String line = "";
         int charRead;
         while ((charRead = in.read()) >= 0) {
            if (charRead == 10) { // newline
               return line;
            }
            line += (char)charRead;
         }
         throw new ClientDataException();
      }

      /**
         Exception to indicate the client sent an empty line of input.
       */
      private static class ClientDataException extends Exception {
      }
   }
}



