package twitter;

import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.endpoints.AdditionalParameters;
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.tweet.TweetV2;
import io.github.redouane59.twitter.dto.user.User;
import io.github.redouane59.twitter.helpers.JsonHelper;
import io.github.redouane59.twitter.signature.TwitterCredentials;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

// TODO: what is the thread safety argument?

/**
 * An object that uses the Twitter API to create and manage subscriptions,
 * return subscribed messages, and retrieve specific Tweets for a user.
 */
public class TwitterListener {
    private final TwitterClient twitterClient;
    private final Set<String> allFeed; //user IDs for full subscriptions
    private final Map<String, List<String>> matchesFeed; //user ID for match subscription, match string list
    private LocalDateTime lastRecentTweetsCall;
    private static final LocalDateTime OCT_1_2022 = LocalDateTime.parse("2022-10-01T00:00:00");

    /**
     * Rep Invariant:
     *      allFeed is a set of valid Twitter user IDs
     *      matchesFeed.keySet() is a set of valid Twitter user IDs
     *      lastRecentTweetsCall is date equal to or later than October 1, 2022
     *
     * Abstraction Function:
     *      Represents a collection of subscriptions to Twitter posts by specific users.
     *
     * Thread Safety:
     *      This class uses the 'synchronized' keyword on certain methods to
     *      avoid mistiming between users when multiple threads are running.
     *      See each method's thread safety on a case by case basis.
     */

    /**
     * Create a TwitterListener object to use the Twitter API and manage subscriptions.
     * @param credentialsFile File object containing API and access token secrets
     */
    public TwitterListener(File credentialsFile) {

        try {
            twitterClient = new TwitterClient(JsonHelper.OBJECT_MAPPER
                    .readValue(credentialsFile, TwitterCredentials.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        allFeed = new HashSet<>();
        matchesFeed = new HashMap<>();

        //Oct 1 treated as default first day to receive Tweets
        lastRecentTweetsCall = OCT_1_2022;
    }

    /**
     * Add a subscription for all tweets made by a specific Twitter user.
     * @param twitterUserName Twitter username to be subscribed to
     * @return true if subscription was added, false if there already
     *          exists a subscription or if the user does not exist
     */
    public boolean addSubscription(String twitterUserName) {
        String followID = getUserID(twitterUserName);

        if (followID == null || allFeed.contains(followID)) {
            return false;
        }

        allFeed.add(followID);
        return true;
    }

    /**
     * Add a subscription for all tweets made by a specific Twitter user that matches a given pattern string.
     * A tweet matches a pattern string if the pattern is a substring of the tweet's text, case-insensitive.
     * @param twitterUserName Twitter username to be subscribed to
     * @param pattern matching string
     * @return true if subscription was added, false if there already
     *          exists a subscription or if the user does not exist
     */
    public boolean addSubscription(String twitterUserName, String pattern) {
        String followID = getUserID(twitterUserName);

        if (followID == null) {
            return false;
        }

        if (matchesFeed.containsKey(followID)){
            if (matchesFeed.get(followID).contains(pattern.toLowerCase())) {
                return false;
            }

            matchesFeed.get(followID).add(pattern.toLowerCase()); //case insensitive
            return true;
        }

        matchesFeed.put(followID, new ArrayList<>(){{
            add(pattern);
        }});
        return true;
    }

    /**
     * Cancels all types of subscriptions to a Twitter user.
     * @param twitterUserName Twitter username to cancel subscription to
     * @return true if a subscription was removed, false otherwise
     */
    public boolean cancelSubscription(String twitterUserName) {
        String cancelID = getUserID(twitterUserName);

        boolean matchSubscription = matchesFeed.containsKey(cancelID);
        boolean allSubscription = allFeed.contains(cancelID);

        if (!(allSubscription || matchSubscription)) {
            return false;
        }

        matchesFeed.remove(cancelID);
        allFeed.remove(cancelID);
        return true;
    }

    /**
     * Cancels a specific user-pattern subscription.
     * @param twitterUserName Twitter username to cancel pattern subscription to
     * @param pattern matching string
     * @return true if the subscription was removed, false otherwise
     */
    public boolean cancelSubscription(String twitterUserName, String pattern) {
        String cancelID = getUserID(twitterUserName);

        if (matchesFeed.containsKey(cancelID) && matchesFeed.get(cancelID).contains(pattern)) {
            matchesFeed.get(cancelID).remove(pattern);
            if (matchesFeed.get(cancelID).isEmpty()) {
                matchesFeed.remove(cancelID);
            }
            return true;
        }
        return false;
    }

    /**
     * Returns all subscribed tweets since the last call to the function.
     * If getRecentTweets is called for the first time, tweets are provided from Oct 1, 2022.
     * @return a List containing all new subscribed Tweets as TweetData objects
     */
    public List<TweetV2.TweetData> getRecentTweets() {
        List<TweetV2.TweetData> tweetData = new ArrayList<>();
        LocalDateTime currentTime = LocalDateTime.now();

        allFeed.forEach(x -> tweetData.addAll(getTweetsByID(x, lastRecentTweetsCall, currentTime)));

        for (String id: matchesFeed.keySet()) {
            for (TweetV2.TweetData data: getTweetsByID(id, lastRecentTweetsCall, currentTime)) {
                for (String pattern: matchesFeed.get(id)) {
                    //remember pattern is stored in lowercase, case-insensitive
                    if (data.getText().toLowerCase().contains(pattern)) {
                        tweetData.add(data);
                        break;
                    }
                    boolean breakdat = false;
                    if (data.getEntities() != null && data.getEntities().getHashtags() != null) {
                        for (TweetV2.HashtagEntityV2 hashtag: data.getEntities().getHashtags()) {
                            if (pattern.equals("#" + hashtag.getText())) {
                                tweetData.add(data);
                                breakdat = true;
                            }
                        }
                    }
                    if (breakdat) {
                        break;
                    }
                }
            }
        }

        lastRecentTweetsCall = currentTime;
        return tweetData;
    }

    /**
     * Returns all Tweets made by a user in a time range.
     * @param twitterUserName Twitter username to get messages from
     * @param startTime beginning of the time range
     * @param endTime end of the time range
     * @return a List containing Tweets by the user within
     *          the time range as TweetData objects
     */
    public List<TweetV2.TweetData> getTweetsByUser(String twitterUserName,
                                                   LocalDateTime startTime,
                                                   LocalDateTime endTime) {
        return getTweetsByID(getUserID(twitterUserName), startTime, endTime);
    }

    /**
     * Returns all Tweets made by a user in a time range.
     * @param twitterID Twitter user ID to get messages from
     * @param startTime beginning of the time range
     * @param endTime end of the time range
     * @return a List containing Tweets by the user within
     *          the time range as TweetData objects
     */
    private List<TweetV2.TweetData> getTweetsByID(String twitterID,
                                                   LocalDateTime startTime,
                                                   LocalDateTime endTime) {
        User twUser = twitterClient.getUserFromUserId(twitterID);
        if (twUser == null) {
            throw new IllegalArgumentException();
        }
        TweetList twList = twitterClient.getUserTimeline(twUser.getId(),
                AdditionalParameters.builder().startTime(startTime).endTime(endTime).build());
        return twList.getData();
    }

    /**
     * Returns the Twitter user ID associated with a Twitter username.
     * @param twitterUserName Twitter username to find ID of
     * @return the ID of the Twitter user
     */
    private String getUserID(String twitterUserName) {
        return twitterClient.getUserFromUserName(twitterUserName).getId();
    }

}
