/*
 * /home/grant/JavaClasses/Comp213/MetaClient/DisplayPanel.java
 *
 * Created: Wed Dec 11 22:52:46 2013
 *
 * copyright Grant Malcolm
 *
 *   This source code may be freely used, modified, or distributed
 *   provided due credit is given.
 *
 */

package Comp213.MetaClient;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JFrame;
import javax.swing.ScrollPaneConstants;

/**
   Thread-safe scrolling area to display text.
   Text can be added {@link #append(String) as a string}
   or {@link #appendChar(char) character-by-character}.
   Adding a line adds a newline at the end of the given text.
   <p>
   The number of lines contained in the text area is fixed;
   as new lines are added, the oldest lines will be removed.
   The number of lines contained can be set in the
   {@link #DisplayPanel(int) constructor}.
   </p><p>
   Newlines can be explicitly displayed as <code>\n</code>
   at the end of a line.
   (This can be useful in applications where it is important to check
   that a newline has been received; e.g., servers whose protocols make use
   of newlines.)
   This feature is set in the {@link #DisplayPanel(boolean) constructor}.
   </p>

   @author <a href="mailto:grant@liverpool.ac.uk">Grant Malcolm</a>
   @version 1.0
 */
public class DisplayPanel extends JPanel {

   // --- Fields --------------------------------------------------------------

   /**
      The default number of rows in the display.
      This value is set to 10.
    */
   public static final int DEFAULT_ROWS = 10;

   /**
      The default number of columns in the display.
      This value is set to 20.
    */
   public static final int DEFAULT_COLS = 20;

   /**
      The default maximum number of lines to be stored.
      This value is set to 30.
    */
   public static final int MAX_LINES = 30;

   /**
      The minimum acceptable value for the number of lines to be stored.
      This value is set to 3.
    */
   public static final int MIN_MAX_LINES = 3;

   /**
      The default setting for whether newlines are to be displayed
      as <code>\n</code>.
    */
   public static final boolean NEWLINES = true;


   /**
      Determines whether to display newlines explicitly as <code>\n</code>.
    */
   private final boolean addNewlines;

   /**
      The {@link JTextArea JTextArea} for the display.
    */
   private final JTextArea textArea;

   /**
      The maximum number of lines to be displayed.
      Must be at least MIN_MAX_LINES.
    */
   private final int maxLines;

   /**
      Number of lines to reduce the text to
      when {@link #maxLines maximum number of lines}
      has been reached.
      Set to 2/3 of {@link #maxLines maximum number of lines}.
    */
   private final int minLines;

   /**
      Number of lines to remove when {@link #maxLines maximum number of lines}
      has been reached.
      Set to {@link #maxLines maxLines} - {@link #minLines minLines}.
    */
   private final int lostLines;

   /**
      The text to be stored.
      Each element of the array is a line, and should not contain "\n".
    */
   private final String[] storedLines;

   /**
      Index of the last line in {@link #storedLines the array}.
    */
   private int currentLineIndex = 0;

   /**
      The number of characters in the text.
    */
   private int size = 0;


   // --- Constructors --------------------------------------------------------

   /**
      Creates a new <code>DisplayPanel</code> instance with the given options.
      The maximum number of lines that can be shown
      before earlier lines are lost should be at least
      {@link #MIN_MAX_LINES 3}.

      @param rows the number of rows in the
                  {@link javax.swing.JTextArea text area}
      @param columns the number of columns in the
                     {@link javax.swing.JTextArea text area}
      @param lines the maximum number of lines stored and available through
                   scrolling; should be at least MIN_MAX_LINES
      @param newlines whether to show newlines explicitly as "\n"

      @throws IllegalArgumentException if <code>lines</code> is less than
        {@link #MIN_MAX_LINES 3}
    */
   public DisplayPanel(final int rows, final int columns,
                       final int lines, final boolean newlines) {
      // do we have enough lines
      if (lines < MIN_MAX_LINES) {
         throw new IllegalArgumentException(
            "number of lines should be at least " + MIN_MAX_LINES);
      }
      // set up scrollable text area
      this.textArea = new JTextArea(rows, columns);
      this.textArea.setEditable(false);
      JScrollPane sPane = new JScrollPane(textArea);
      sPane.setHorizontalScrollBarPolicy(
         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      sPane.setVerticalScrollBarPolicy(
         ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      this.add(sPane);
      // apply options
      this.maxLines = lines;
      this.minLines = (2 * this.maxLines) / MIN_MAX_LINES;
      this.lostLines = this.maxLines - this.minLines;
      this.addNewlines = newlines;
      this.storedLines = new String[lines];
      this.storedLines[this.currentLineIndex] = "";
   }

   /**
      Creates a new <code>DisplayPanel</code> instance
      with the given numbers of rows and columns,
      and the given maximum number of lines to be kept available.

      @param rows the number of rows in the
                  {@link javax.swing.JTextArea text area}
      @param columns the number of columns in the
                     {@link javax.swing.JTextArea text area}
      @param lines the maximum number of lines stored and available through
                   scrolling; should be at least {@link #MIN_MAX_LINES 3}

      @throws IllegalArgumentException if <code>lines</code> is less than
        {@link #MIN_MAX_LINES 3}
    */
   public DisplayPanel(final int rows, final int columns, final int lines) {
      this(rows, columns, lines, NEWLINES);
   }

   /**
      Creates a new <code>DisplayPanel</code> instance with the given options.

      @param rows the number of rows in the
                  {@link javax.swing.JTextArea text area}
      @param columns the number of columns in the
                     {@link javax.swing.JTextArea text area}
      @param newlines whether to show newlines explicitly as "\n"
    */
   public DisplayPanel(final int rows, final int columns,
                       final boolean newlines) {
      this(rows, columns, MAX_LINES, newlines);
   }

   /**
      Creates a new <code>DisplayPanel</code> instance with the given options.

      @param lines the maximum number of lines stored and available through
                   scrolling; should be at least {@link #MIN_MAX_LINES 3}
      @param newlines whether to show newlines explicitly as "\n"

      @throws IllegalArgumentException if <code>lines</code> is less than
        {@link #MIN_MAX_LINES 3}
    */
   public DisplayPanel(final int lines, final boolean newlines) {
      this(DEFAULT_ROWS, DEFAULT_COLS, MAX_LINES, newlines);
   }

   /**
      Creates a new <code>DisplayPanel</code> instance that displays
      the iven number of rows and columns.

      @param rows the number of rows in the
                  {@link javax.swing.JTextArea text area}
      @param columns the number of columns in the
                     {@link javax.swing.JTextArea text area}
    */
   public DisplayPanel(final int rows, final int columns) {
      this(rows, columns, MAX_LINES, true);
   }

   /**
      Creates a new <code>DisplayPanel</code> instance with the option
      of displaying newlines explicitly as "\n" at the end of a line.

      @param newlines whether to show newlines explicitly as "\n"
    */
   public DisplayPanel(final boolean newlines) {
      this(DEFAULT_ROWS, DEFAULT_COLS, newlines);
   }

   /**
      Creates a new <code>DisplayPanel</code> instance
      with the given maximum number of lines to store.
      This number should be at least MIN_MAX_LINES.

      @param lines the maximum number of lines stored and available through
                   scrolling; should be at least MIN_MAX_LINES

      @throws IllegalArgumentException if <code>lines</code> is less than
        {@link #MIN_MAX_LINES 3}
    */
   public DisplayPanel(final int lines) {
      this(DEFAULT_ROWS, DEFAULT_COLS, lines, true);
   }

   /**
      Creates a new <code>DisplayPanel</code> instance with all options
      set to default values.
    */
   public DisplayPanel() {
      this(DEFAULT_ROWS, DEFAULT_COLS, MAX_LINES, NEWLINES);
   }

   // --- public methods ------------------------------------------------------

   /**
      Add a single character to the displayed text.

      @param c the character to add
    */
   public synchronized void appendChar(final char c) {
      if (c == '\n') { // char to add is the newline character
         // add a newline
         this.addNewline();
         // if the array of lines is full, crop lines
         if (++this.currentLineIndex == this.maxLines) {
            this.cropLines();
         }
         // reset unfinished line
         this.storedLines[this.currentLineIndex] = "";
         // make the text area scroll to the bottom
         this.textArea.setCaretPosition(size);
      } else {
         // add the character to array and text area
         this.storedLines[this.currentLineIndex] += c;
         this.textArea.append("" + c);
         this.size++;
      }
   }

   /**
      Append text to the display.

      @param text the text to add to the display
    */
   public synchronized void append(final String text) {
      int len = text.length();
      // append each char
      for (int i = 0; i < len; i++) {
         this.appendChar(text.charAt(i));
      }
   }

   /**
      Clear the displayed text.
    */
   public synchronized void clear() {
      this.currentLineIndex = 0;
      this.storedLines[this.currentLineIndex] = "";
      this.textArea.setText("");
      this.size = 0;
   }

   // --- private methods -----------------------------------------------------

   /**
      Add a newline to the displayed text.
    */
   private void addNewline() {
      if (this.addNewlines) { // showing newlines as "\n"
         this.textArea.append("\\n");
         this.size += 2; // added two characters
      }
      this.textArea.append("\n");
      this.size++; // added one character
   }

   /**
      Crop the stored text.
      Remove {@link #lostLines lostLines} from the stored text.
    */
   private void cropLines() {
      // clear the text display
      this.textArea.setText("");
      this.size = 0;
      /* move minLines of text to start of array
       */
      this.currentLineIndex = 0;    // first position to copy to
      int offset = this.lostLines;  // first position to copy from
      while (this.currentLineIndex < this.minLines) {
         // move a line and save
         this.textArea.append(
            this.storedLines[this.currentLineIndex] =
               this.storedLines[offset]
         );
         this.size += this.storedLines[this.currentLineIndex].length();
         // add newline
         this.addNewline();
         // move on to next line
         this.currentLineIndex++;
         offset++;
      }
      // update the text area
      this.textArea.setCaretPosition(this.size);
   }

   // --- testing -------------------------------------------------------------

   /**
      Test.

      @param args not used
      @exception InterruptedException if interrupted - won't happen
    */
   public static void main(String[] args) throws InterruptedException {
      JFrame f = new JFrame("DisplayPanel Test");
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      DisplayPanel dp = new DisplayPanel(10, 20, 20);
      f.add(dp);
      f.pack();
      f.setVisible(true);
      byte[] bs;
      long start = System.currentTimeMillis();
      for (int i = 0; i < 50; i++) {
         dp.append("" + i);
         dp.appendChar('\n');
            Thread.sleep(100);
      }
      System.out.println("took " + (System.currentTimeMillis() - start) + " millis");
   }
}
