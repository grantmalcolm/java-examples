/*
 * /home/grant/JavaClasses/Comp213/ChatServer/ChatterList.java
 *
 * Created: Fri Oct  2 18:34:08 2009
 * Last modified: 2/10/2009
 *
 * copyright Grant Malcolm
 *
 *   This source code may be freely used, modified, or distributed
 *   provided due credit is given.
 *
 */

package Comp213.ChatServer;

import java.net.Socket;

/**
 * List of Chatters in a chatroom.
 * The requirements for this class are described in the
 * <a href="/~grant/Teaching/COMP213/Ass2/">COMP213 assignment</a>.
 *
 * @author <a href="mailto:grant@liverpool.ac.uk">Grant Malcolm</a>
 * @version 1.0
 */
public class ChatterList 
{

   /**
    * Prefix for broadcast messages that indicate a Chatter has joined
    * the chatroom.
    * When a {@link Chatter Chatter} is added to the list,
    * the new Chatter's name is broadcast to all other Chatters in the room,
    * preceded by this constant.
    *
    */
   private static final char JOIN_PREFIX = '0';

   /**
    * Prefix for broadcast messages that indicate a Chatter has left
    * the chatroom.
    * When a {@link Chatter Chatter} is removed from the list, the
    * leaving Chatter's name is broadcast to all other Chatters in the room,
    * preceded by this constant.
    *
    */
   public static final char LEAVE_PREFIX = '1';

   /**
    * Prefix for each line of a message to be broadcast.
    * This is the prefix for all lines of messages sent to or sent by
    * the chatroom server,
    *
    */
   public static final char MSG_PREFIX = '2';

   /**
    * Signals the end of a multi-line message.
    *
    */
   public static final char MSG_END = '3';

   /**
    * The list of {@link Chatter live Chatters} in the chatroom.
    *
    */
   private Chatter liveChatters = null;

   /**
    * A list of Chatter instances that have left the chatroom.
    *
    */
   private Chatter deadChatters = null;

   /**
    * A list of Chatter instances that have not yet been added to
    * the chatroom because they are waiting for a
    * {@link Chatter#name user name} to be supplied by the remote user.
    *
    */
   private Chatter pendingChatters = null;

   /**
    * Creates a new <code>ChatterList</code> instance.
    * This implements the constant "empty" in the
    * <a href="chatroom.maude">ChatterList specification</a>.
    * The constructor does nothing, as {@link liveChatters liveChatters}
    * is already null, representing the empty list.
    *
    */
   public ChatterList()
   {
   }

   /**
    * Add a Chatter to the list.
    * The Chatter should be in the pending list;
    * remove it and add to the live list.
    *
    * @param c the Chatter to be added
    */
   private void add(Chatter c)
   {
      Chatter tmp = pendingChatters;
      Chatter prev = null;

      /*
       * Traverse the pending list; remove c and add it to
       * the live chatter list
       *
       */

      // have we removed Chatter c? - not yet
      boolean notRemoved = true;

      /*
       * go through the list looking for Chatter c.
       *
       */
      while (tmp != null && notRemoved)  // exit loop at end,
      {                                 // or once c is has been removed
         // not at end of list, and we haven't yet removed c

         // is this c?
         if (tmp == c)
         {
            // we've found c, so remove it
            if (prev == null)
            {
               /*
                * we're at the start of the list; c is the first Chatter,
                * so set the "liveChatters" field to the next node
                *
                */
               pendingChatters = tmp.getNext();
            }
            else
            {
               /*
                * we're not at the start; prev stores the node before tmp.
                * Redirect prev's next pointer to miss out tmp
                *
                */
               prev.setNext(tmp.getNext());
            }
            // c has been removed
            notRemoved = false;  // so we'll exit the loop
         }

         // move prev and tmp on
         prev = tmp;
         tmp = tmp.getNext();
      } // end loop to find c

      /*
       * If c was in the pending list, it's been removed;
       * add it to the live Chatters
       */
      c.setNext(liveChatters);
      liveChatters = c;
   }

   /**
    * Send a message to all {@link Chatter Chatters} in the chatroom.
    * This implements the operation "broadcast" in the
    * <a href="chatroom.maude">ChatterList specification</a>.
    * The protocol describing the format of messages is described in the
    * <a href="/~grant/Teaching/COMP213/Ass1/">COMP213 assignment</a>.
    *
    * @param msg the message to be sent to all remote users
    */
   private void broadcast(String msg)
   {
      // variable for traversing the linked list
      Chatter tmp = liveChatters;

      /*
       * traverse the list
       */
      while (tmp != null) // exit loop at the end of the list
      {
         // send the message
         tmp.sendToUser(msg);

         // and move on to the next chatter in the list
         tmp = tmp.getNext();
      }
   }

   /**
    * Send a message from one remote user to all chatters in the chatroom.
    *
    * @param cName the name of the chatter sending the message
    * @param msg the message text - all lines should be prefixed by
    *            {@link #MSG_PREFIX MSG_PREFIX} 
    */
   public synchronized void sendMsg(String cName, String msg)
   {
      broadcast(MSG_PREFIX + cName + "\n" + msg + "\n" + MSG_END);
   }

   /**
    * Add a Chatter to the chatroom, and inform all other Chatters.
    * This implements the operation "connect" in the
    * <a href="chatroom.maude">ChatterList specification</a>.
    * The Chatter's name is broadcast to all other {@link Chatter Chatters}
    * in the chatroom, preceded by {@link JOIN_PREFIX the appropriate prefix}.
    *
    * @param c the Chatter joining the chatroom
    */
   public synchronized void connect(Chatter c)
   {
      // inform all other chatters
      broadcast(JOIN_PREFIX + c.getName());

      // and add the new Chatter to the list
      add(c);
   }

   /**
    * Remove a Chatter from the chatroom, and inform all other Chatters.
    * This implements the operation "leave" in the
    * <a href="chatroom.maude">ChatterList specification</a>.
    * The Chatter's name is broadcast to all other {@link Chatter Chatters}
    * in the chatroom, preceded by {@link LEAVE_PREFIX the appropriate prefix}.
    *
    * @param c the Chatter leaving the chatroom
    */
   public synchronized void leave(Chatter c)
   {
      // use this variable to traverse the linked list
      Chatter tmp = liveChatters;

      /*
       * we need to remove c when we find it, which means we need to redirect
       * the "next" pointer of the node *before* c to the node after c.
       * This node stores the node before tmp - if it's null, then we're
       * still at the start of the list.
       *
       */
      Chatter prev = null;

      /*
       * Traverse the list - we go through the list, sending messages
       * to the remote users as we go, until we find the
       * first occurrence of the Chatter in the list, and remove it.
       * (We assume there is only one occurrence of any Chatter in the list.)
       * Once we have removed the Chatter, we go through the remainder of
       * the list sending messages, and not checking if we need to remove
       * Chatter c.
       *
       */

      // have we removed Chatter c? - not yet
      boolean notRemoved = true;

      /*
       * go through the list looking for Chatter c,
       * sending messages as we go
       *
       */
      while (tmp != null && notRemoved)  // exit loop at end,
      {                                 // or once c is has been removed
         // not at end of list, and we haven't yet removed c

         // is this c?
         if (tmp == c)
         {
            // we've found c, so remove it
            if (prev == null)
            {
               /*
                * we're at the start of the list; c is the first Chatter,
                * so set the "liveChatters" field to the next node
                *
                */
               liveChatters = tmp.getNext();
            }
            else
            {
               /*
                * we're not at the start; prev stores the node before tmp.
                * Redirect prev's next pointer to miss out tmp
                *
                */
               prev.setNext(tmp.getNext());
            }
            // c has been removed
            notRemoved = false;  // so we'll exit the loop

            tmp = tmp.getNext();
            // add c to deadChatters list
            addDeadChatter(c);
            break;
         }
         else
         {
            System.out.println("sending to " + tmp.getName());
            // current node is not c: just send the message to this chatter
            tmp.sendToUser(LEAVE_PREFIX + c.getName());
         }

         // move prev and tmp on
         prev = tmp;
         tmp = tmp.getNext();
      } // end loop to find c

      /*
       * either we're at the end of the list, or c has been removed.
       * In either case, send the message to all remaining chatters
       * (none, if we're at the end of the list!).
       *
       */
      while (tmp != null)  // exit loop at end of list
      {
         tmp.sendToUser(LEAVE_PREFIX + c.getName());
         tmp = tmp.getNext();
      }
   }

   /**
    * Add to the list of chatters that have left the chatroom.
    * These Chatter instances can be re-used when new users join
    * the chatroom,
    *
    * @param c a chatter that has left the chatroom
    */
   private void addDeadChatter(Chatter c)
   {
      /*
       * add c to the start of the list; the next Chatter will
       * be the Chatter that had been first
       */
      c.setNext(deadChatters);
      deadChatters = c;
   }

   /**
    * Get a Chatter instance.
    * To avoid creating unnecessary objects, we re-use Chatter instances.
    * The list {@link #deadChatters deadChatters} stores old Chatter instances;
    * we either re-use one of these, or, if there are none, create a new
    * Chatter instance.
    *
    * @param sock the Socket representing the connection to the remote user
    * @return a <code>Chatter</code> value
    */
   public synchronized Chatter getChatter(Socket sock)
   {
      // value to be returned
      Chatter nextChatter;

      if (deadChatters == null)
      {
         // no dead chatters available, so create a new one
         nextChatter = new Chatter(this);
      }
      else
      {
         // get (and remove) the first chatter in the list
         nextChatter = deadChatters;
         deadChatters = nextChatter.getNext();
      }

      // attach the chatter to the remote client
      nextChatter.setClient(sock);

      // add the chatter to the pending list
      nextChatter.setNext(pendingChatters);
      pendingChatters = nextChatter;

      return nextChatter;
   }

   /**
    * The number of live Chatters in the chatroom.
    *
    * @return the number of live chatters in the chatroom
    */
   public synchronized int length()
   {
      int len = 0;
      Chatter c = liveChatters;
      while (c != null)
      {
         len++;
         c = c.getNext();
      }
      return len;
   }

   /**
    * Close all connections to all remote clients.
    *
    */
   public synchronized void shutDown()
   {
      Chatter c = liveChatters;
      while (c != null)
      {
         c.close();
         c = c.getNext();
      }

      // don't forget the pending chatters
      c = pendingChatters;
      while (c != null)
      {
         c.close();
         c = c.getNext();
      }
   }
}

