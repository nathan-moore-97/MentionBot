

//Imports for System IO
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

//Imports for reply storage and use
import java.util.ArrayList;
import java.util.Random;

//Imports for the Twitter4j Library
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Class MentionBot - the purpose of this class is to handles the TwitterAPI I/O
 * to stream tweets from the home feed and post a mention. 
 * 
 * MODIFICATIONS - NAM 25.10.2017: Program now accepts a command line argument
 *                 that gets set as the Target User. There is no current 
 *                 verification in place that checks if the target user 
 *                 actually exists.
 *                 
 *                 NAM 26.10.2017: MentionBot now will only add a message to 
 *                 the ArrayList if the length of the message AND the
 *                 "@targetScreenName" is less than or equal to 140 characters  
 *                 
 *                 NAM 14.12.2017: Made some changes to the way its set up to 
 *                 remove any association to the twitter account it used to be 
 *                 linked to - moved authentication keys into a new class. 
 *                 
 * 
 * @author nathanmoore
 * @version 3.0 December 14th, 2017
 */

public class MentionBot {
    
    // Functional Declarations    
    private int cycles;
    private int failures;
    private int lastReplyIndex; 
    private int replies;
    private ArrayList<String> messages;
    
    // Twitter Declarations
    private String targetScreenName;
    private Status mostRecentReply;
    private ConfigurationBuilder cb;
    private TwitterFactory tf;
    private Twitter twitter;
    
    
    /**
     * Twitter Keys is a class that contains the Authentication Keys that 
     * allow MentionBot to post to Twitter, and nothing else.
     */
    private TwitterKeys keys;
    
    /**************************** Startup Methods *************************/
    
    /**
     * MentionBot Constructor - initializes and instantiates the class level
     * attributes. 
     * 
     * TODO - It may be a good idea incorporate file reading and writing to 
     * set the initial state of lastReplyIndex, to lessen the chance of 
     * MentionBot throwing a TwitterException on startup, due to the message 
     * being posted the same as the last one
     * 
     * @param userScreenName 
     */
    private MentionBot( String userScreenName ) {
        
        // initial states for the bot
        replies = 0;
        failures = 0;
        lastReplyIndex = -1; 
        cycles = 0;
        
        keys = new TwitterKeys();
        
        messages = new ArrayList<String>();
        populateMessages();
        
        //Twitter stuff
        cb = new ConfigurationBuilder();
        
        cb.setDebugEnabled( true ).setOAuthConsumerKey( keys.getConsumerKey() )
        .setOAuthConsumerSecret( keys.getConsumerSecret() )
        .setOAuthAccessToken( keys.getOAuthToken() )
        .setOAuthAccessTokenSecret( keys.getOAuthTokenSecret() );
        
        tf  = new TwitterFactory( cb.build() );
        twitter = tf.getInstance();
        
        
        targetScreenName = userScreenName;
        
    }

    /************************* MentionBot Main *************************/
    
    /**
     * main method- accepts an argument from the command line and will set that 
     * String argument to the name that MentionBot targets, alternatively if no 
     * argument is passed the program will prompt the Authenticating user for a 
     * String input and will use that to target a user.
     * 
     * @param args 
     * @throws IOException 
     */
    public static void main( String[] args ) throws IOException {
        String name = "";
        
        if ( args.length == 1 ) {
            name = args[0];
        } else {
            BufferedReader reader = 
                    new BufferedReader( new InputStreamReader( System.in ) );
            
            System.out.println( "Please enter the Screen Name of a user you "
                    + "would like \nMentionBot to reply to." );
            
            System.out.print( ">> @" );
            name = reader.readLine();
        }
        
        new MentionBot( name ).run();
    }

    /************************* MentionBot IO Methods ************************/
    
    /**
     * getMostRecentTweet - returns the most recent tweet in the authenticating
     * users timeline.
     * 
     * @return the most recent tweet
     * @throws TwitterException 
     */
    private Status getMostRecentTweet() throws TwitterException {
        return twitter.getHomeTimeline().get( 0 );
    }
    
    /**
     * Posts a mention to the specified user's timeline.
     * 
     * @param s (Status) - the status being replied to 
     * @throws TwitterException 
     */
    private void postMention( Status s ) throws TwitterException {
        twitter.updateStatus( "@" + targetScreenName + getStatusString() );
        replies++;  
    }

    /**
     * sends a report of a TwitterException to a user via direct message.
     * 
     * @param e 
     */
    private void sendErrorReport( TwitterException e ) {
        System.out.println( e.getErrorMessage() );
        failures++;
        try {
            twitter.sendDirectMessage( "someUsername", e.getErrorMessage() );
        } catch ( TwitterException e1 ) {
            System.out.println( e1.getErrorMessage() );
            failures++;
        } 
    }

    /********************** MentionBot Control Methods ***********************/
    
    /**
     * actOn - takes in the most recent status and decides what to do.  
     * 
     * @param mostRecent 
     * @throws TwitterException 
     */
    private void actOn( Status mostRecent ) throws TwitterException {
        if ( !mostRecent.equals( mostRecentReply ) ) {
            if ( mostRecent.getUser().getScreenName()
                    .equals( targetScreenName ) ) {
                postMention( mostRecent );
                mostRecentReply = mostRecent;
            }
        }   
    }

    /**
     * add() - this method adds messages to the messages ArrayList, only if
     * the total length with the target name is less than 140 characters.
     * 
     * @param s - message to be added
     */
    private void add( String s ) {
        if ( ( "@" + targetScreenName + s ).length() <= 140 ) {
            messages.add( s );
        }
    }

    /**
     * ClearScreen - prints 25 blank lines to the console to clear it. 
     * Method is designed for a standard terminal window of 80 x 24
     */
    private void clearScreen() {
        
        for ( int i = 0; i < 25; i++ ) {
            System.out.print( "\n" );
        }
        
    }

    /**
     * cycle - prints out relevant information and then waits for one minute
     * and then iterates. 
     */
    private void cycle() {
        try {
            
            clearScreen();
            push();
            cycles++;
            
            // this is to avoid hitting 
            // the getTimeline rate limit, 
            // which is 15 requests per 15 minutes
            Thread.sleep( 1000 * 60 ); 
            
        } catch ( InterruptedException e ) {  }
        
    }

    /**
     * gets a String that MentionBot will post.
     * 
     * @return a string to post
     */
    private String getStatusString() {
        Random rand = new Random();
        int choice = -1;
        
        do {
            choice = rand.nextInt( messages.size() );
        } while ( choice == lastReplyIndex );
        
        return messages.get( choice );
    }

    /**
     * populateMessages - this method holds all of the replies that MentionBot
     * can use. Since messages is an ArrayList, this allows the Authenticating
     * User to add as many messages as they want.
     */
    private void populateMessages() {
    
        add( "Messages that the developer wants MentionBot to post" );
        add( "as many as you want" );
    }

    /**
     * Prints meta data to the console.
     */
    private void push() {
    
        System.out.println( "Replies to @" + targetScreenName + ": " 
                + replies + " " );
        System.out.println( "MentionBot has been "
                + "running for: " + cycles + " cycles" );
        System.out.println( "Currently listening for user "
                + "@" + targetScreenName + "..." );
        
    }

    /**
     * run - main control method for MentionBot.
     */
    private void run() {
       /* 
        * if MentionBot keeps encountering Exceptions of any kind, I want it
        * to shut down 
        */
        while ( failures < 5 ) {
            try {
                actOn( getMostRecentTweet() );
                cycle();
                
            } catch ( TwitterException e ) {
                sendErrorReport( e );
            }
        }
    }

}

