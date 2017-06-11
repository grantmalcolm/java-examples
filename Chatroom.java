/*
 * /home/grant/JavaClasses/Comp213/ChatServer/Chatroom.java
 *
 * Created: Wed Nov  4 18:18:20 2009
 *
 * copyright Grant Malcolm
 *
 *   This source code may be freely used, modified, or distributed
 *   provided due credit is given.
 *
 */

package Comp213.ChatServer;

import java.net.ServerSocket;
import java.net.Socket;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.Thread.State;
import java.util.List;


/**
 * Chatroom server for COMP213 assignment.
 * Sets up a server socket on port 12000 by default;
 * specify a port nnnn by using <code>-port nnn</code>
 * as a command-line option.
 * Shut the server down by connecting to the port,
 * entering any non-empty line, followed by the line
 * <code>.die</code>.
 *
 * @author <a href="mailto:grant@liverpool.ac.uk">Grant Malcolm</a>
 * @version 2.0
 */
public class Chatroom
{
   /**
    * Code to be entered by remote user to shut down the server.
    *
    */
   public static final String QUITC = ".die";


   /**
    * Port number for the server socket. Default is 12000.
    *
    */
   private static int portNum = 12000;


   /**
    * The server socket.
    *
    */
   public static ServerSocket theSocket;


   /**
    * Indicates that the server has received a request to shut down.
    *
    */
   private static boolean closeReq = false;


   /**
    * Used to shut the server down.
    *
    */
   public static void close()
   {
      // flag that shutdown has been requested
      closeReq = true;
      System.out.println("Shutting the Comp213 Chatroom down");

      try
      {
         /* close the server socket;
          * the accept()-loop in main will throw
          * a SocketException
          */
         theSocket.close();
      }
      catch (IOException ioe)
      {
         if (theSocket != null && !theSocket.isClosed())
         {
            // something's gone badly wrong
            System.out.println("Can't close the Server Socket");
            System.out.println("Trusting the interpreter to shut down nicely");
            System.exit(1);
         }
      }
   }


   /**
    * Set up the Chatroom.
    *
    * @param args command-line options:
    *  <ul>
    *   <li> <code>-port nnn</code> - run server on port number nnn;
    *     default is 12000
    *  </ul>
    */
   public static void main(String[] args)
   {
      // check if port number is specified
      if (args.length > 0)
      {
         if (args.length == 2 && args[0].equals("-port"))
         {
            try 
            {
               // 2nd argument should be the port number
               portNum = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException nfe)
            {
               // bad command-line arguments
               System.err.println("invalid port number");
               System.exit(2);
            }
         }
         else
         {
            // only accept "-port nnn" as valid command-line arguments
            System.err.println("usage: -port n");
            System.exit(2);
         }
      }

       // set up ChatterList
      //
      ChatterList chatters = new ChatterList();


       // set up server socket
      //
      try  // set up server
      {
         theSocket = new ServerSocket(portNum);
      }
      catch (IOException ioe)
      {
         // fatal error; print info...
         System.err.println("Comp213 Chatroom: "
                            + "could not create server socket");
         System.err.println(ioe.getMessage());
         System.exit(1);
      }
      System.out.println("Server socket has been started on port "+portNum);

       // Thread-pool for session-handlers
      //
      ExecutorService threadPool = Executors.newCachedThreadPool();

      /*
       * Start loop to accept incoming connections.
       * For each incoming socket connection, start up a Chatter.
       *
       */
      Socket incoming = null;     // new connection.

      boolean incomingOK = true;  // is connection established?
                                 // Keep this true by default;
                                // set to false on error.

      while (true)  // keep accepting requests
      {
         try
         {
            // to get next client
            incoming = theSocket.accept( );
         }
         catch (IOException ioe)
         {
            // Is error fatal?
            if (theSocket == null || theSocket.isClosed())
            {
               // no server socket: shut the chatroom down

               /*
                * wait for any recent connections to be processed
                */
               int repeatWaits = 0;  // timeout
               while (Thread.activeCount() > chatters.length() + 1
                      && repeatWaits < 2) // do this at most twice
               {
                  System.out.println((repeatWaits > 0) ?
                                     "waiting for queued logins to complete" :
                                     "last chance for queued logins");
                  try 
                  {
                     Thread.sleep(100);
                  }
                  catch (InterruptedException ie)
                  {
                     // shouldn't happen
                  }
                  repeatWaits++;
               } // all connections should now be processed

               // close connections
               chatters.shutDown();

               if (closeReq) // chatroom has been asked to shut down
               {
                  if (Thread.activeCount() == 1)
                  {
                     // only main thread left
                     System.out.println("Comp213 Chatroom: closed.");
                     return;
                  }
                  List<Runnable> laggards = threadPool.shutdownNow();
                  System.out.println(laggards.size() + " threads left");
                  // else: still some threads to shut down
                  System.out.println("Comp213 Chatroom: closing connections");

                  int ts = Thread.activeCount();
                  Thread[] ats = new Thread[ts];
                  Thread.enumerate(ats);
                  // give each thread a chance to end
                  for (int i = 0 ; i < ts ; i++)
                  {
                     if (! ats[i].getName().equals("main"))
                     {
                        System.out.println("found thread: " + ats[i].getName());
                        System.out.println("it is: " + ats[i].getState());
                        ats[i].interrupt();
                        try{
                           ats[i].setPriority(Thread.MAX_PRIORITY);
                           Thread.sleep(100);
                        } catch (InterruptedException ieee) {}
                     }
                  }

                  // in case this hasn't worked
                  if (Thread.activeCount() > 1)
                  {
                     System.out.println("Comp213 Chatroom: "
                                        +"some threads have not ended...");
                     System.out.println("...exiting java interpreter");
                     System.exit(1);
                  }

                  // else: everything as it should be
                  System.out.println("Comp213 Chatroom: closed.");
                  return;
               }
               /*
                * If we're here, then not everything is as it should be
                */
               System.err.println("COMP213 Chatroom: fatal error -");
               System.err.println("   "+ioe.getMessage());
               System.exit(1);
            }
            /*
             * otherwise: non-fatal error,
             * just the incoming socket died:
             */
            incomingOK = false;
         } // end catch-block for I/O errors

          // only create the client Chatter if there was no IOException
         //
         if (incomingOK)
         {
            // start new thread to serve new client
            threadPool.execute(chatters.getChatter(incoming));
         }
         else // there was an IOException; wait for new incoming
         {
            incomingOK = true;  // back to default setting
         }
      }  // end while loop
   } // end main
   
} // Chatroom

