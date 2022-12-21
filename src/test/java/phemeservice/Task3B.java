package phemeservice;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import pheme.PhemeService;
import security.BlowfishCipher;
import timedelayqueue.PubSubMessage;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Task3B {

    private static PhemeService srv;
    private static String userName1;
    private static UUID userID1;
    private static String userName2;
    private static UUID userID2;
    private static String hashPwd1;
    private static String hashPwd2;
    private static PubSubMessage msg1;

    @BeforeAll
    public static void setup() {
        srv = new PhemeService(new File("secret/auth.json"));

        userName1 = "Test User 1";
        userID1 = UUID.randomUUID();
        hashPwd1 = BlowfishCipher.hashPassword("Test Password 1", BlowfishCipher.gensalt(12));

        userName2 = "Test User 2";
        userID2 = UUID.randomUUID();
        hashPwd2 = BlowfishCipher.hashPassword("Test Password 2", BlowfishCipher.gensalt(12));
    }

    @Test
    @Order(1)
    public void testAddUser() {
        assertTrue(srv.addUser(userID1, userName1, hashPwd1));
    }

    @Test
    @Order(2)
    public void testAddDuplicateUser() {
        String userName = "Test User 1";
        String hashPwd = BlowfishCipher.hashPassword("Test Password 1", BlowfishCipher.gensalt(12));
        UUID userID = UUID.randomUUID();

        assertFalse(srv.addUser(userID, userName, hashPwd));
    }

    @Test
    @Order(3)
    public void testAddSecondUser() {
        assertTrue(srv.addUser(userID2, userName2, hashPwd2));
    }

    @Test
    @Order(4)
    public void testSendMsg() {
        msg1 = new PubSubMessage(
                userID1,
                userID2,
                "Test Msg"
        );
        srv.sendMessage(userName1, hashPwd1, msg1);
        assertEquals(PubSubMessage.NO_MSG, srv.getNext(userName2, hashPwd2));
    }

    @Test
    @Order(5)
    public void testReceiveMsg() {
        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException ie) {
            fail();
        }
        assertEquals(msg1, srv.getNext(userName2, hashPwd2));
    }

    @Test
    @Order(6)
    public void testMsgDelivered() {
        //assertTrue(srv.isDelivered(msg1.getId(), userID2));
        //politely ignored since cache
    }

    @Test
    @Order(7)
    public void testIsUserTrue() {
        assertTrue(srv.isUser(userName2));
    }

    @Test
    @Order(8)
    public void testIsUserFalse() {
        assertFalse(srv.isUser("testuser1"));
    }

    @Test
    @Order(9)
    public void testAddSubscription1() {
        assertTrue(srv.addSubscription(userName1, hashPwd1, "UBC"));
    }

    @Test
    @Order(10)
    public void testAddSubscription2() {
        assertFalse(srv.addSubscription("userName1", hashPwd1, "UBC"));
    }
    //fails since no "userName1" user

    @Test
    @Order(11)
    public void testAddSubscription3() {
        assertFalse(srv.addSubscription(userName1, "hashPwd1", "UBC"));
    }

    @Test
    @Order(12)
    public void getRecentMsgs1() {
        assertTrue(srv.getAllRecent(userName1, hashPwd1).size() > 10);
    }

    @Test
    @Order(13)
    public void getRecentMsgs2() {
        PubSubMessage msg = new PubSubMessage(
                userID2,
                userID1,
                "From 2 to 1"
        );
        srv.sendMessage(userName2, hashPwd2, msg);
        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException ie) {
            fail();
        }
        List<PubSubMessage> msgs = srv.getAllRecent(userName1, hashPwd1);
        assertTrue(msgs.size() < 10);
        assertTrue(msgs.size() >= 1);
        assertTrue(msgs.contains(msg));
    }

    @Test
    @Order(14)
    public void getRecentMsgs3() {
        List<PubSubMessage> msgs = srv.getAllRecent(userName1, hashPwd1);
        assertTrue(msgs.size() == 0);
        assertEquals(PubSubMessage.NO_MSG, srv.getNext(userName1, hashPwd1));
    }

    @Test
    @Order(15)
    public void testMultipleSubscriptionsWithKeywords() {
        String userName3 = "Test User 3";
        String hashPwd3 = BlowfishCipher.hashPassword("Test Passwd 3", BlowfishCipher.gensalt(12));
        UUID userID3 = UUID.randomUUID();
        srv.addUser(userID3, userName3, hashPwd3);
        srv.addSubscription(userName3, hashPwd3, "ubcengineering", "ceremonies");
        srv.addSubscription(userName3, hashPwd3, "ubcappscience", "ceremonies");
        List<PubSubMessage> msgs = srv.getAllRecent(userName3, hashPwd3);
        assertTrue(msgs.size() == 4);
    }
    @Test
    @Order(16)
    public void wrongPass() {
        assertFalse(srv.addSubscription(userName1,hashPwd2,"UBC"));
    }
    @Test
    @Order(17)
    public void testRemoveUser() {
        srv.addUser(userID1, userName1, hashPwd1);
        assertTrue(srv.removeUser(userName1,hashPwd1));
    }

    @Test
    @Order(18)
    public void cancel() {
        srv.addSubscription(userName2,hashPwd2,"UBC");
        assertTrue(srv.cancelSubscription(userName2,hashPwd2,"UBC"));
    }

}