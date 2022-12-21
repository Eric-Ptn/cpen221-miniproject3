package pheme;

import io.github.redouane59.twitter.dto.tweet.TweetV2;
import security.BlowfishCipher;
import timedelayqueue.BasicMessageType;
import timedelayqueue.MessageType;
import timedelayqueue.PubSubMessage;
import timedelayqueue.TimeDelayQueue;
import twitter.TwitterListener;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;

/**
 * This is the backend of an interactive platform where users
 * have the ability to communicate with each other. Within this class,
 * users can access their subscriptions on Twitter, and view their
 * recent messages from other users.
 * <p>
 * User "feed" (the term we will use to represent the messages and
 * tweets each user has access to) is displayed to them upon request
 * in chronological order.
 * <p>
 * This class works in tandem with the TwitterListener class, and allows users
 * to interact with what they request from the Twitter API.
 */
public class PhemeService {

    public static final int DELAY = 1000; // 1 second or 1000 milliseconds
    private final File twitterCredentialsFile;
    private Map<UUID, String> phemePassword;
    private Map<String, UUID> phemeUser;
    private Map<String, TwitterListener> userInterfaces;
    private Map<String, TimeDelayQueue> userMessages;

    /**
     * Rep Invariant:
     *         A user has a user ID and username that are both UNIQUE - non
     *         repeating.
     *
     * Abstract Function:
     *
     *         This represents the backend (or "service") that allows users to
     *         communicate with each other. Here, we send messages pop items
     *         off the TwitterListener TimeDelayQueue, and allow users to
     *         send messages to one another.
     * Thread Safety:
     *         This class uses the 'synchronized' keyword on certain methods to
     *         avoid mistiming between users when multiple threads are running.
     *
     *         This is particularly true when the users are sending messages to
     *         one another.
     *
     *         See each method's thread safety on a case by case basis.
     */


    /**
     * The PhemeService Constructor creates an instance of PhemeService
     * <p>
     * This instance contains a Map of a String and List
     * <p>
     * The String represents the user interacting with the pheme service
     * The list contains a salt (a type of encryption key), and
     * the user's hashed password (both in String form)
     */
    public PhemeService(File twitterCredentialsFile) {
        this.twitterCredentialsFile = twitterCredentialsFile;

        phemePassword = new HashMap<>();
        phemeUser = new HashMap<>();
        userInterfaces = new HashMap<>();
        userMessages = new HashMap<>();
    }

    /**
     * Add a new user to a map of ids and passwords to manage their interactions on
     * the pheme service
     * <p>
     * Moreover, we need to create an instance of the TwitterListener for
     * the user who wants to interact with Twitter
     * <p>
     * @param userID The ID of the new user
     * @param userName The username of the new user
     * @param hashPassword The encrypted password of the user to add
     * @return true if added, false if already in the map
     */
    public boolean addUser(UUID userID, String userName, String hashPassword) {

        if (phemeUser.containsKey(userName)) {
            return false;
        }

        //map the password to userID
        phemePassword.put(userID, hashPassword);
        phemeUser.put(userName, userID);

        //map userName to a new instance of TwitterListener
        TwitterListener userProfile = new TwitterListener(twitterCredentialsFile);
        userInterfaces.put(userName, userProfile);

        //map username to a new instance of the TimeDelayQueue
        TimeDelayQueue userQueue = new TimeDelayQueue(DELAY);
        userMessages.put(userName, userQueue);

        return true;
    }

    /**
     * Remove a user from the map of users which manages their interactions on
     * the pheme service
     * @param userName The username of the user to remove
     * @param hashPassword The hashed password of the user to remove
     * @return true if removed, false if unable to remove or does not exist in the map
     */
    public boolean removeUser(String userName, String hashPassword) {

        //return false if the user does not exist
        if (!phemeUser.containsKey(userName)) {
            return false;
        }

        //see if the password is correct and return true or false based on outcome
        if (verifyPass(userName, hashPassword)) {

            //kill the user and their id
            phemeUser.remove(userName);

            //kill the id and their password
            UUID userID = phemeUser.get(userName);
            phemePassword.remove(userID);

            //remove user from userInterfaces as well
            userInterfaces.remove(userName);

            //kill the user and their messages
            userMessages.remove(userName);

            return true;
        }
        else {
            return false;
        }

    }

    /**
     * Remove a subscription to the tweets of a certain user
     * @param userName The username of the client that will be used to verified with the password
     * @param hashPassword The hashed password of the client
     * @param twitterUserName The name of the user the client want to unsubscribe from
     * @returns false if the username of the client and password does not match, true if
     *          they do match and the subscription is removed
     */
    public boolean cancelSubscription(String userName,
                                      String hashPassword,
                                      String twitterUserName) {

        //verify password
        if (!verifyPass(userName, hashPassword)) {
            return false;
        }

        //cancel the subscription
        userInterfaces.get(userName).cancelSubscription(twitterUserName);

        return true;
    }

    /**
     * Remove a subscription to the tweets of a certain user that contains the matching string
     * @param userName The username of the client that will be used to verified with the password
     * @param hashPassword The hashed password of the client
     * @param twitterUserName The name of the user the client wants to unsubscribe from
     * @param pattern matching String in the tweet, a substring of the tweet's text
     * @return false if the username of the client and password does not match, true if they do
     *          match and the subscription is removed
     */
    public boolean cancelSubscription(String userName,
                                      String hashPassword,
                                      String twitterUserName,
                                      String pattern) {

        //verify password
        if (!verifyPass(userName, hashPassword)) {
            return false;
        }

        //cancel the subscription
        userInterfaces.get(userName).cancelSubscription(twitterUserName, pattern);

        return true;
    }


    /**
     * Add a subscription to the tweets of a certain user
     * @param userName The username of the client that will be used to verified with the password
     * @param hashPassword The hashed password of the client
     * @param twitterUserName The name of the user the client wants to subscribe to
     * @return false if the username of the client and password does not match,
     *          true if they do match and the subscription is added
     */
    public boolean addSubscription(String userName, String hashPassword,
                                   String twitterUserName) {

        //verify password
        if (!verifyPass(userName, hashPassword)) {
            return false;
        }

        //add a subscription to the user's profile
        userInterfaces.get(userName).addSubscription(twitterUserName);

        return true;
    }

    /**
     * Add a subscription to the tweets of a certain user that contains the matching string
     * @param userName The username of the client that will be used to verified with the password
     * @param hashPassword The hashed password of the client
     * @param twitterUserName The name of the user the client wants to subscribe to
     * @param pattern matching String in the tweet, a substring of the tweet's text
     * @return false if the username of the client and password does not match, true if
     *          they do match and the subscription is added
     */
    public boolean addSubscription(String userName, String hashPassword,
                                   String twitterUserName,
                                   String pattern) {
        //verify password
        if (!verifyPass(userName, hashPassword)) {
            return false;
        }

        //add a subscription to the user's profile
        userInterfaces.get(userName).addSubscription(twitterUserName, pattern);

        return true;
    }

    /**
     * Send a message to a user
     * @param userName The username of the client that will be used to verified with the password
     * @param hashPassword The hashed password of the client
     * @param msg the message that is being sent
     * @return false if the username of the client and password does not match, true if they do match and the message is sent
     */
    public synchronized boolean sendMessage(String userName,
                               String hashPassword,
                               PubSubMessage msg) {

        //verify password
        if (!verifyPass(userName, hashPassword)) {
            return false;
        }

        List<UUID> receiverID = msg.getReceiver();
        List<String> receiverNames = new ArrayList<>();

        for (UUID ID: receiverID) {
            for (String username: phemeUser.keySet()) {
                if (phemeUser.get(username).equals(ID)) {
                    receiverNames.add(username);
                    break;
                }
            }
        }

        for(String name: receiverNames){
            TimeDelayQueue userQueue = userMessages.get(name);

            if (phemeUser.containsKey(name)) {
                //send the message
                userQueue.add(msg);
            }
        }

        return true;
    }

    /**
     * Check if a list of messages are delivered
     * @param msgID the ID of the message
     * @param userList The list containing all the ID of the messages within it
     * @return A List of booleans that represent if the provided list of messages are sent or not, true if that message is sent and false if not
     */
    public synchronized List<Boolean> isDelivered(UUID msgID, List<UUID> userList) {
        List<Boolean> deliverydelivery = new ArrayList<>();
        userList.forEach(x -> deliverydelivery.add(isDelivered(msgID, x)));
        return deliverydelivery;
    }

    /**
     * Checks if the message is sent to a specific user
     * @param msgID The ID of the message that is being checked whether is it delivered or not
     * @param user The user the message is sent to
     * @return true if the user has received the message and false if not
     */
    public synchronized boolean isDelivered(UUID msgID, UUID user) {
        String username = "";

        for (String name: phemeUser.keySet()) {
            UUID currentID = phemeUser.get(name);
            if (currentID.equals(user)) {
                username = name;
                break;
            }
        }

        return userMessages.get(username).getMsg(msgID);
    }


    /**
     * Check if someone exists in this instance of PhemeService
     * @param userName the username that is checked for existence
     * @return true if exists, false otherwise
     */
    public boolean isUser(String userName) {
        return phemeUser.containsKey(userName);
    }

    /**
     * Get the most recent tweet or message, whatever is latest
     * @param userName The username of the client that will be used to verified with the password
     * @param hashPassword The hashed password of the client
     * @return The most recent tweet/message
     */
    public PubSubMessage getNext(String userName, String hashPassword) {

        if (!verifyPass(userName, hashPassword)) {
            return PubSubMessage.NO_MSG;
        }

        PubSubMessage mostRecentMsg = userMessages.get(userName).getNext();
        List<TweetV2.TweetData> tweetData = userInterfaces.get(userName).getRecentTweets();

        if (tweetData.isEmpty()) {
            return mostRecentMsg;
        }

        PubSubMessage mostRecentTweet = tweetDat(userInterfaces.get(userName).getRecentTweets().get(0));

        if (mostRecentTweet.getTimestamp().after(mostRecentMsg.getTimestamp())) {
            return mostRecentMsg;
        }

        return mostRecentTweet;
    }

    /**
     * Collect all recent tweets and messages from a user
     * @param userName The username of the client that will be used to verified with the password
     * @param hashPassword The hashed password of the client
     * @return A list that contains all the recent messages and tweets of a user in chronological order
     */
    public List<PubSubMessage> getAllRecent(String userName, String hashPassword) {
        // should be tweets AND messages, CLEARS APPROPRIATE TDQ (without violating delay)
        // you get to choose order

        if (!verifyPass(userName, hashPassword)) {
            return List.of(PubSubMessage.NO_MSG);
        }

        PriorityQueue<PubSubMessage> msgQueue = new PriorityQueue<>(new PubSubMessageComparator());

        while (!userMessages.get(userName).peek().equals(PubSubMessage.NO_MSG)) {
            msgQueue.add(userMessages.get(userName).getNext());
        }

        PriorityQueue<PubSubMessage> tweetQueue = new PriorityQueue<>(new PubSubMessageComparator());
        tweetQueue.addAll(userInterfaces.get(userName).getRecentTweets().stream().map(this::tweetDat).toList());

        List<PubSubMessage> allRecent = new ArrayList<>();

        while (!(msgQueue.isEmpty() && tweetQueue.isEmpty())) {
            if (msgQueue.isEmpty()) {
                allRecent.add(tweetQueue.poll());
                continue;
            }

            if (tweetQueue.isEmpty()) {
                allRecent.add(msgQueue.poll());
                continue;
            }

            if (msgQueue.peek().getTimestamp().after(tweetQueue.peek().getTimestamp())) {
                allRecent.add(tweetQueue.poll());
            } else {
                allRecent.add(msgQueue.poll());
            }
        }

        return allRecent;
    }

    /**
     * Convert a tweet into a PubSubMessage
     * @param twitt The tweet that needs to be converted
     * @return A PubSubMessage version of the tweet
     */
    private PubSubMessage tweetDat (TweetV2.TweetData twitt) {
        return new PubSubMessage(
                UUID.randomUUID(),
                Timestamp.valueOf(twitt.getCreatedAt()),
                UUID.randomUUID(),
                UUID.randomUUID(),
                twitt.getText(),
                BasicMessageType.SIMPLEMSG
        );
    }

    /**
     * Compares an inputted password to the cached password associated with a user's
     * account.
     * @param userName username of person's password to verify
     * @param hashPassword inputted password to compare to cached hashed password
     * @return true if inputted password and cached password are the same, false otherwise
     */
    private boolean verifyPass (String userName, String hashPassword) {

        UUID userID = phemeUser.get(userName);
        if (userID == null) {
            return false;
        }

        String comparePass = phemePassword.get(userID);
        return comparePass.equals(hashPassword);

    }

    // a comparator to sort messages from earliest to latest
    private class PubSubMessageComparator implements Comparator<PubSubMessage> {
        public int compare(PubSubMessage msg1, PubSubMessage msg2) {
            return msg1.getTimestamp().compareTo(msg2.getTimestamp());
        }
    }
}