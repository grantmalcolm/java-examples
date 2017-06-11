/*
 * /home/grant/JavaClasses/Comp213/ChatServer/Chatter.java
 *
 * Created: Tue Nov  3 16:53:58 2009
 *
 * copyright Grant Malcolm
 *
 *   This source code may be freely used, modified, or distributed
 *   provided due credit is given.
 *
 */

package Comp213.ChatServer;

import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Chatters in a chatroom.
 * This class also provides a linked list structure.
 *
 * @author <a href="mailto:grant@liverpool.ac.uk">Grant Malcolm</a>
 * @version 1.0
 */
public class Chatter
   implements Runnable
{

   /**
    * The next chatter in a list.
    * This field provides the linked-list structure.
    *
    */
   private Chatter next = null;

   /**
    * The Socket connection to the remote client.
    *
    */
   private Socket client = null;

   /**
    * The output stream to the remote client.
    *
    */
   private PrintWriter out;

   /**
    * The input stream from the remote client.
    *
    */
   private BufferedReader in;

   /**
    * The chosen name of the remote user.
    *
    */
   private String name;

   /**
    * The list of Chatters in the chatroom.
    *
    */
   private ChatterList chatroom;


   /**
    * Creates a new <code>Chatter</code> with a reference to the
    * chatroom.
    *
    * @param cl the list of chatters that this chatter will join.
    */
   public Chatter(ChatterList cl)
   {
      chatroom = cl;
   }

   /**
    * Set the Socket connection to the remote client.
    *
    * @param sock a <code>Socket</code> connection to a remote client.
    */
   public void setClient(Socket sock)
   {
      client = sock;
   }

   /**
    * Get the Name of the Chatter.
    *
    * @return the Chatter's name
    */
   public String getName()
   {
      return name;
   }

   /**
    * Get the next Chatter in the list.
    *
    * @return the next Chatter in the list
    */
   public final Chatter getNext()
   {
      return next;
   }

   /**
    * Set the next Chatter in the list.
    *
    * @param newNext the Chatter instance that follows this one in the list
    */
   public final void setNext(final Chatter newNext)
   {
      this.next = newNext;
   }

   /**
    * Send a message to the remote client.
    *
    * @param msg the message to be sent to the remote client
    */
   public void sendToUser(String msg)
   {
      out.println(msg);
      out.flush();
   }

   /**
    * Handle a session with a remote client.
    *
    */
   public void run()
   {
      try 
      {
         out = new PrintWriter(
                     new OutputStreamWriter(client.getOutputStream()));
         in =  new BufferedReader(
                     new InputStreamReader(client.getInputStream()));

         // get user name
         String line = in.readLine();
         if (line.trim().equals(""))
         {
            /*
             * empty user name, so end the session;
             * NB - the "finally"-clause will be invoked,
             * so this thread will be removed from the chatroom
             */
            return;
         }
         name = line;

         // now we're up and running; connect to the chatterlist
         chatroom.connect(this);

         // to store messages received from the remote client
         String msg = "";

         // have we read all the lines of a message
         boolean done = false;

         // now just listen for messages and respond
         while ((line = in.readLine()) != null)
         {
            if (line.trim().equals(""))
            {
               // bad input from user; end session
               return;
            }
            else if (line.charAt(0) == ChatterList.MSG_PREFIX)
            {
               // add line to msg
               msg = line;

               done = false;
               while (! done)
               {
                  line = in.readLine();
                  if (line == null || line.trim().equals(""))
                  {
                     // bad protocol
                     return;
                  }
                  if (line.charAt(0) == ChatterList.MSG_PREFIX)
                  {
                     // add the current line to the message
                     msg += "\n" + line;
                  }
                  else if (line.charAt(0) == ChatterList.MSG_END)
                  {
                     // end of message; exit loop then send to all
                     done = true;
                  }
                  else
                  {
                     // any other option is bad protocol
                     return;
                  }
               }

               // message from remote client
               chatroom.sendMsg(name, msg);
            }
            else if (line.equals(Chatroom.QUITC))
            {
               // shut down the chatroom
               Chatroom.close();

               // our work is done
               return;
            }
            else if (line.charAt(0) == ChatterList.LEAVE_PREFIX)
            {
               // remote user logging out
               return;
            }
            else
            {
               // either bad protocol, or user is quitting; end session
               return;
            }
         }
         // input line was null; end session
      }
      catch (IOException ioe)
      {
         System.out.println("IOException: " + ioe.getMessage());
      }
      finally
      {
         // shut down I/O
         close();
         // die and exit the chatroom
         chatroom.leave(this);
      }
   }

   /**
    * Close network connections to remote client.
    *
    */
   public void close()
   {
      if (out != null)
      {
         out.close();
      }
      if (in != null)
      {
         try
         {
            in.close();
         }
         catch(IOException ioe)
         {}
      }
      if (client != null) // it will be, but...
      {
         try
         {
            client.close();
         }
         catch (IOException ioe)
         {
            // nothing useful to do
         }
         client = null;
      }
   }
}
