/*
 *  /home/grant/JavaClasses/Comp213/LikeTunes/TuneList.java
 *
 *  Created: Fri Dec  6 23:44:05 2013
 *
 *  copyright Grant Malcolm
 *
 *    This code is a model solution for an assignment
 *    used in COMP213 at the University of Liverpool;
 *    please do not archive or distribute this file or its contents.
 *
 *  To do:
 *    - get rid of inner class Node: move links to inner class Tune
 */

package Comp213.LikeTunes;


/**
 *  List of tunes for a Like-Tunes server.
 *  Tunes are stored by alphabetical order by artist's name then title.
 *  Tunes are also ranked by popularity:
 *  the popularity of a tune is incremented by the
 *  {@link #likeTune(String,String) likeTune() method}.
 *
 *
 *  @author <a href="mailto:grant@liverpool.ac.uk">Grant Malcolm</a>
 *  @version 1.0
 */
public class TuneList {

   /**
    *  Class of Tunes.
    *  A tune consists of the {@link Tune#artist name of the artist},
    *  the {@link Tune#title title},
    *  and the {@link Tune#likes number of likes}.
    *
    *  @author <a href="mailto:grant@liverpool.ac.uk">Grant Malcolm</a>
    *  @version 1.0
    */
   public static class Tune implements Comparable<Tune> {

      /**
       *  The name of the artist.
       */
      private final String artist;

      /**
       *  The title of the tune.
       */
      private final String title;

      /**
       *  The popularity rating of the tune.
       */
      private int likes = 0 ;

      /**
       *  Pointer to the next Node in alphabetic order.
       *
       */
      private Tune nextByArtist;

      /**
       *  Pointer to the next Node in decreasing order of poularity.
       *
       */
      private Tune nextByLikes;

      /**
       *  Pointer to the previous Node in decreasing order of popularity;
       *  i.e., this points to a more popular tune.
       *
       */
      private Tune prevByLikes;

      /**
       *  Creates a new <code>Tune</code> instance
       *  with a given artist, title and ID;
       *  the popularity rating is set to 0.
       *
       *  @param artist the artist
       *  @param title the title
       */
      public Tune(final String artist, final String title) {

         this.artist = artist;
         this.title = title;
      }

      /**
       *  Like this tune.
       *
       */
      public final void like() {

         this.likes++;
      }

      /**
       *  Test whether this tune has a given artist and title.
       *
       *  @param artist an artist name
       *  @param title a title
       *
       *  @return true if the artist and title are those of the tune;
       *    false otherwise
       */
      public boolean equals(final String artist, final String title) {

         return this.artist.equals(artist) && this.title.equals(title);
      }

      /**
       *  Compare this tune to another by alphabetic order of
       *  artist then title.
       *  Compares the artist names; if these are equal, the order will then
       *  be the alphabetic order of titles.
       *
       *  @param t the tune to compare this instance to
       *  @return 0 if the artist names and titles are equal;
       *    a negative value if the artist name of the instance alphabetically
       *    precedes the artist name of the parameter, or if the artist names
       *    are the same and the title of the instance precedes that of the
       *    parameter;
       *    a positive value if the artist name of the parameter precedes that
       *    of the instance, or if the artist names are equal and the title of
       *    the parameter precedes that of the instance
       */
      public int compareTo(Tune t) {

         return (artist + title).compareTo(t.artist + t.title);
         /*
         int v = artist.compareTo(t.artist);
         if (v == 0) {
            return title.compareTo(t.title);
         }
         return v;
         */
      }

      /**
       *  Compare the popularity of tunes.
       *  Order by popularity; if the two tunes have the same number
       *  of likes, order alphabetically.
       *
       *  @param t the tune to compare this tune to
       *  @return 0 if the tunes are equal;
       *    a negative value if this tune is less popular than
       *    the parameter tune;
       *    a positive value if this tune is more popular than
       *    the parameter tune
       */
      public int compareToByLikes(Tune t) {
         // get the difference in popularity values
         int diff = this.likes - t.likes;
         // if they're the same, order alphabetically
         if (diff == 0) {
            return t.compareTo(this);
         }
         return diff;
      }

      /**
       *  Give a string representation of the tune
       *  with artist, title and number of likes on separate lines.
       *
       *  @return a string containing the tune data
       */
      public String toString() {

         return artist + "\n" + title + "\n" + likes + "\n";
      }
   } // end inner class Tune


   /**
    *  First {@link TuneList.Tune Tune} in alphabetic order.
    *
    */
   private Tune firstByArtist;

   /**
    *  First {@link TuneList.Tune Tune} in oder of popularity.
    *
    */
   private Tune firstByLikes;

   /**
    *  Last {@link TuneList.Tune Tune} in oder of popularity.
    *
    */
   private Tune lastByLikes;


   /**
    * Creates an empty list of tunes.
    *
    */
   public TuneList() {

      //  firstByArtist, firstByLikes, and lastByLikes all null
   }

   /**
    *   Add a new tune to the list.
    *   The tune is specified by giving the
    *   {@link TuneList.Tune#artist artist} and
    *   {@link TuneList.Tune#title title};
    *   the {@link TuneList.Tune#likes popularity} of the tune is 0.
    *   This method has no effect if the tune already exists in the list.
    *
    *   @param artist the artist's name
    *   @param title the title of the tune
    */
   public void addTune(String artist, String title) {
      // the node to insert
      Tune n = new Tune(artist, title);

      /*
       *   first add the new tune in alphabetical order
       */
      if (firstByArtist == null) {
         // nothing in the list; add the new tune
         firstByArtist = firstByLikes = lastByLikes = n;
      } else {
         /*  at least one tune in the list;
          *  find where to put the new tune in alphabetic order
          */
         Tune current = firstByArtist;  // to traverse the list

         // does the new tune go right at the start of the list?
         if (firstByArtist.compareTo(n) > 0) {
            // the new tune should be first in the alphabetical list
            n.nextByArtist = firstByArtist;
            firstByArtist = n;
         } else {
            // find the insertion point
            while (current.nextByArtist != null
                   && current.nextByArtist.compareTo(n) <= 0) {
               /*
                *  tune after current tune is smaller than or equal to
                *  tune to add; move on
                */
               current = current.nextByArtist;
            }
            /*
             *  either current.nextByArtist is null or
             *  is greater than tune to add;
             *  if current is equal, don't add
             */
            if (current.artist.equals(n.artist)
                && current.title.equals(n.title)) {
               // work is already in the list: do nothing
               return;
            }
            n.nextByArtist = current.nextByArtist;
            current.nextByArtist = n;
         }

         /*  we've added a new tune:
          *  now fix popularity links;
          *  new tune has sold nothing,
          *  so should be placed near the end of the list;
          *  start at the end and work backwards
          */
         current = lastByLikes;
         // is the new tune the least popular?
         if (n.compareToByLikes(lastByLikes) < 0) {
            // add right at the end
            lastByLikes.nextByLikes = n;
            n.prevByLikes = lastByLikes;
            lastByLikes = n;
         } else {
            // move back to find the insertion point
            while (current != null &&
                   n.compareToByLikes(current) > 0) {
               current = current.prevByLikes;
            }
            /*  found the insertion point;
             *  current is either null (new tune is now most popular)
             *  or more popular than the new tune:
             *  in either case, current precedes the new tune
             */
            n.prevByLikes = current;
            if (current == null) {
               // set n to be the most popular
               n.nextByLikes = firstByLikes;
               firstByLikes.prevByLikes = n;
               firstByLikes = n;
            } else {
               // n comes right after current
               n.nextByLikes = current.nextByLikes;
               if (current.nextByLikes != null) {
                  current.nextByLikes.prevByLikes = n;
               }
               current.nextByLikes = n;
            }
         } // end find insertion point in popularity
      } // end if-else for at least one tune in list
   }

   /**
    *  Add to the popularity of a tune.
    *  This method increments the {@link TuneList.Tune#likes popularity level}
    *  of the tune with a given {@link TuneList.Tune#artist artist name}
    *  and {@link TuneList.Tune#title title}.
    *
    *  @param artist the name of the artist of the tune to like
    *  @param title the title of the tune to like
    */
   public void likeTune(final String artist, final String title) {

      /*  find the tune to buy;
       *  reasonable guess is that the tune being bought is popular,
       *  so start at most popular
       */
      Tune n = firstByLikes;  // to traverse the list:
      boolean notFoundYet = true;  // set to false when the tune is found
      // find the tune and like it
      while (n != null && notFoundYet) {
         if (n.equals(artist, title)) {
            // found the tune; increment its popularity ...
            n.like();
            // ... and exit loop
            notFoundYet = false;
         } else {
            // not the one that I want, oo oo oo - so move on
            n = n.nextByLikes;
         }
      }
      // did we find the tune?
      if (n == null) {
         // nope
         return;
      }
      /*  else: move the tune to the correct place
       *  in the popularity stakes;
       *  start by looking at the node that was previous to n
       */
      Tune current = n.prevByLikes;  // was more popular
      // is there anything there?
      if (current == null) {
         // n was already most popular; no need to do anything
         return;
      }
      // is it more popular than n?
      if (current.compareToByLikes(n) > 0) {
         // n is already in the correct place
         return;
      }
      // else: remove n
      current.nextByLikes = n.nextByLikes;
      if (current.nextByLikes == null) {
         // n was lastByLikes
         lastByLikes = current;
      } else {
         current.nextByLikes.prevByLikes = current;
      }
      /*  n is now removed from the list;
       *  now find where to re-insert it
       */
      while (current != null && current.compareToByLikes(n) < 0) {
         // move past any less popular tunes
         current = current.prevByLikes;
      }
      /*  current is either null or more popular than n
       */
      if (current == null) {
         // n is now most popular
         n.nextByLikes = firstByLikes;
         n.prevByLikes = null;
         firstByLikes.prevByLikes = n;
         firstByLikes = n;
      } else {
         // put n after current
         n.prevByLikes = current;
         n.nextByLikes = current.nextByLikes;
         current.nextByLikes = n;
         if (n.nextByLikes != null) {
            n.nextByLikes.prevByLikes = n;
         }
      }
      // and we're done
   }

   /**
    *  Generate a string containing each tune in the list,
    *  in alphabetical order.
    *
    *  @return the string with all tunes in the list
    */
   public String listAlphabetically() {

      // the string containing data on all the tunes in the list
      String data = "";
      /*  now go through the list,
       *  ading each tune to this string
       */
      Tune n = firstByArtist;  // to traverse the list
      while (n != null) {
         data += n;       // add the current tune
         n = n.nextByArtist;  // and move on to next
      }
      // n == null; at end of list
      return data;
   }

   /**
    *  Generate a string with all tunes in the list,
    *  in decreasing order of popularity.
    *
    *  @return the string with all tunes, in decreasing order of popularity
    */
   public String listByLikes() {

      String data = "";  // the string to return
      /*  now go through the list,
       *  ading each tune to this string
       */
      Tune n = firstByLikes;  // to traverse the list
      while (n != null) {
         data += n;       // add the current tune
         n = n.nextByLikes;  // and move on to next
      }
      // n == null; at end of list
      return data;
   }

   public static void main(String[] args) {
      
      TuneList tl = new TuneList();
      tl.addTune("Deerhoof", "We do Parties");
      System.out.println(tl);
      tl.addTune("FKA Twigs", "Hours");
      System.out.println(tl);
      tl.likeTune("Deerhoof", "We do Parties");
      System.out.println(tl);
      tl.addTune("Deerhoof", "Flower");
      tl.addTune("Deerhoof", "Flower");
      System.out.println(tl);
      tl.addTune("Deerhoof", "We do Parties");
      tl.addTune("FKA Twigs", "Hours");
      System.out.println(tl);
      tl.likeTune("hjlk", "hjl");

      System.out.println(tl);
      System.out.println(tl.listByLikes());

      tl.likeTune("Deerhoof", "Flower");

      System.out.println(tl);
      System.out.println(tl.listByLikes());

      tl.addTune("The Necks", "Rum Jungle");
      tl.likeTune("The Necks", "Rum Jungle");

      System.out.println(tl);
      System.out.println(tl.listByLikes());

      tl.likeTune("The Necks", "Rum Jungle");
      tl.likeTune("Deerhoof", "We do Parties");
      tl.addTune("Manu Chao", "Les Milles Paillettes");
      tl.likeTune("The Necks", "Rum Jungle");

      System.out.println(tl.listAlphabetically());
      System.out.println(tl.listByLikes());
   }
}
