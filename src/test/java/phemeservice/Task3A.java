package phemeservice;

import io.github.redouane59.twitter.dto.tweet.Tweet;
import io.github.redouane59.twitter.dto.tweet.TweetV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import twitter.TwitterListener;

import java.io.File;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class Task3A {

    TwitterListener bluebird;
    @BeforeEach
    public void setup() {
        File credentialsFile = new File("secret/auth.json");
        bluebird = new TwitterListener(credentialsFile);
    }

    @Test
    public void getOurLatestTweet() {
        List<TweetV2.TweetData> amjadList = bluebird.getTweetsByUser("FrcParent", null, null);
        Tweet twitt = amjadList.get(0);
        System.out.println(twitt.getText());
        assertEquals("can't wait to be in gneshin impact!!! \uD83E\uDD23", twitt.getText());
    }

    //remember getUserTimeline only gets ~3200 most recent tweets
    @Test
    public void getElonsTweets() {
        LocalDateTime start = LocalDateTime.of(2022,
                Month.MAY, 26, 0, 0);

        LocalDateTime end = LocalDateTime.of(2022,
                Month.DECEMBER, 4, 0, 0);

        List<TweetV2.TweetData> elonList = bluebird.getTweetsByUser("elonmusk", start, end);

        System.out.println("Elon Musk has't tweet'd " + elonList.size() + " times.");

        elonList.forEach(x -> System.out.println(x.getText()));
    }

    @Test
    public void matchTweets() {

        String pattern = "tesla";
        bluebird.addSubscription("elonmusk", pattern);

        List<TweetV2.TweetData> elonList = bluebird.getRecentTweets();
        System.out.println("Elon Musk has't tweet'd about " + pattern + " " + elonList.size() + " times.");
        elonList.forEach(x -> {
            System.out.println(x.getText());
            assertTrue(x.getText().toLowerCase().contains(pattern));
        });

        // we just called getRecentTweets
        int shouldBeZero = bluebird.getRecentTweets().size();
        assertEquals(0, shouldBeZero);

        bluebird.cancelSubscription("elonmusk");
    }

    @Test
    public void normalSubscribe() {

        assertTrue(bluebird.addSubscription("FrcParent"));

        List<TweetV2.TweetData> frcList = bluebird.getRecentTweets();
        frcList.forEach(x -> System.out.println(x.getText()));

        assertTrue(bluebird.cancelSubscription("FrcParent"));
    }

    @Test
    public void cancellingWrongSubscriptions() {

        assertTrue(bluebird.addSubscription("FrcParent"));
        assertFalse(bluebird.cancelSubscription("FrcParent", "wait"));

        List<TweetV2.TweetData> frcList = bluebird.getRecentTweets();
        frcList.forEach(x -> System.out.println(x.getText()));

        assertTrue(bluebird.cancelSubscription("FrcParent"));
    }

    @Test
    public void cancelSubscription() {

        assertTrue(bluebird.addSubscription("FrcParent", "Genshin"));
        assertTrue(bluebird.cancelSubscription("FrcParent"));

        List<TweetV2.TweetData> frcList = bluebird.getRecentTweets();
        assertTrue(frcList.size() == 0);

        assertFalse(bluebird.cancelSubscription("FrcParent"));
    }
    @Test
    public void testFetchRecentTweets() {
        bluebird.addSubscription("UBC");
        List<TweetV2.TweetData> tweets = bluebird.getRecentTweets();
        tweets.forEach(x -> System.out.println(x.getText()));
        assertTrue(tweets.size() > 0);
        bluebird.cancelSubscription("UBC");
    }

    @Test
    public void testDoubleFetchRecentTweets() {
        bluebird.addSubscription("UBC");
        List<TweetV2.TweetData> tweets = bluebird.getRecentTweets();
        tweets.forEach(x -> System.out.println(x.getText()));
        assertTrue(tweets.size() > 0);
        tweets = bluebird.getRecentTweets();
        assertTrue(tweets.size() == 0); // second time around, in quick succession, no tweet
        bluebird.cancelSubscription("UBC");
    }

    @Test
    public void wrongFilePath() {
        try{
            TwitterListener redbird = new TwitterListener(new File("secret/fake.json"));
            assertTrue(false);
        } catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    @Test
    public void addExistingSubscription() {
        assertTrue(bluebird.addSubscription("elonmusk"));
        assertFalse(bluebird.addSubscription("elonmusk"));
        assertTrue(bluebird.cancelSubscription("elonmusk"));

        assertTrue(bluebird.addSubscription("UBC", "research"));
        assertFalse(bluebird.addSubscription("UBC", "research"));
        assertTrue(bluebird.cancelSubscription("UBC", "research"));
    }

    @Test
    public void addSubscriptionToNonexistent() {
        assertFalse(bluebird.addSubscription(
                "AFKJKL:HKALGBKHJAJFHJDBHABKSNJKDGIJABUF*#U*YUFADHIKGNJH"));
        assertFalse(bluebird.addSubscription(
                "AFKJKL:HKALGBKHJAJFHJDBHABKSNJKDGIJABUF*#U*YUFADHIKGNJH", "bruh"));
        try {
            bluebird.getTweetsByUser(
                    "AFKJKL:HKALGBKHJAJFHJDBHABKSNJKDGIJABUF*#U*YUFADHIKGNJH", null, null);
        } catch (RuntimeException e) {
            assertTrue(true);
        }
    }

    @Test
    public void addDifferentPatternSubscriptions() {
        String pattern1 = "tesla";
        String pattern2 = "spacex";
        bluebird.addSubscription("elonmusk", pattern1);
        bluebird.addSubscription("elonmusk", pattern2);

        List<TweetV2.TweetData> elonList = bluebird.getRecentTweets();
        System.out.println("Elon Musk has't tweet'd about " + pattern1
                + " and " + pattern2 + " " + elonList.size() + " times.");
        elonList.forEach(x -> {
            System.out.println(x.getText());
            assertTrue(x.getText().toLowerCase().contains(pattern1) ||
                    x.getText().toLowerCase().contains(pattern2));
        });

        bluebird.cancelSubscription("elonmusk");
    }

    @Test
    public void nonTrollTest() {
        String pattern = "#T20WorldCup";
        bluebird.addSubscription("HariBalakrish20", pattern);

        List<TweetV2.TweetData> hariList = bluebird.getRecentTweets();
        System.out.println("Hari has't tweet'd about " + pattern
                + " " + hariList.size() + " times.");
        hariList.forEach(x -> {
            System.out.println(x.getText());
        });

        bluebird.cancelSubscription("HariBalakrish20");

    }

}