package timedelayqueue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class Task2 {
    private static final int DELAY    = 40; // delay of 40 milliseconds
    private static final int NUM_MSGS = 10;

    private static final Gson gson;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.serializeNulls();
        gson = gsonBuilder.create();
    }

    private class Sender implements Runnable {
        private int id;
        private TimeDelayQueue tdq;
        private List<PubSubMessage> msgList;

        public Sender(int id, TimeDelayQueue tdq, List<PubSubMessage> msgList) {
            this.id      = id;
            this.tdq     = tdq;
            this.msgList = msgList;
        }

        public void run() {
            int msgsAdded = 0;
            for (int i = 0; i < Task2.NUM_MSGS; i++) {
//                System.out.printf("Thread %d : Message %d\n", id, i);
                UUID sndID        = UUID.randomUUID();
                UUID rcvID        = UUID.randomUUID();
                String text       = gson.toJson("loren ipsum");
                PubSubMessage msg = new PubSubMessage(sndID, rcvID, text);
                msgList.add(msg);
                if (tdq.add(msg)) {
                    msgsAdded++;
                }
                try {
                    Thread.sleep(1);
                }
                catch (InterruptedException ie) {
                    fail();
                }
            }
//            System.out.printf("Thread %d added %d messages\n", id, msgsAdded);
        }
    }

    private class Receiver implements Runnable {
        private TimeDelayQueue tdq;
        private List<PubSubMessage> msgList;

        public Receiver(TimeDelayQueue tdq, List<PubSubMessage> msgList) {
            this.tdq     = tdq;
            this.msgList = msgList;
        }

        public void run() {
            try {
                Thread.sleep(DELAY * Task2.NUM_MSGS * 2);
            }
            catch (InterruptedException ie) {
                fail();
            }

            for (int i = 0; i < Task2.NUM_MSGS; i++) {
                PubSubMessage msg = tdq.getNext();
                assertEquals(msgList.get(i), msg);
            }
        }
    }

    @Test
    public void testAddAndGet() {
        TimeDelayQueue tdq          = new TimeDelayQueue(DELAY);
        List<PubSubMessage> msgList = new ArrayList<>();

        Thread writer = new Thread(new Sender(0, tdq, msgList));
        Thread reader = new Thread(new Receiver(tdq, msgList));
        writer.start();
        reader.start();
    }

    @Test
    public void testTwoAdders() {
        TimeDelayQueue tdq          = new TimeDelayQueue(DELAY);
        List<PubSubMessage> msgList = new ArrayList<>();
        final int NUM_WRITERS = 10;

        Thread[] writerArray = new Thread[NUM_WRITERS];

        for (int i = 0; i < NUM_WRITERS; i++) {
            writerArray[i] = new Thread(new Sender(i, tdq, msgList));
        }

        for (int i = 0; i < NUM_WRITERS; i++) {
            writerArray[i].start();
        }

        for (int i = 0; i < NUM_WRITERS; i++) {
            try {
                writerArray[i].join();
            }
            catch (InterruptedException ie) {
                fail();
            }
        }

        assertEquals(NUM_WRITERS * NUM_MSGS, tdq.getTotalMsgCount());
    }

    @Test
    public void testPeakLoad() {
        TimeDelayQueue tdq          = new TimeDelayQueue(DELAY);
        List<PubSubMessage> msgList = new ArrayList<>();
        final int NUM_WRITERS = 10;

        Thread[] writerArray = new Thread[NUM_WRITERS];

        for (int i = 0; i < NUM_WRITERS; i++) {
            writerArray[i] = new Thread(new Sender(i, tdq, msgList));
        }

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUM_WRITERS; i++) {
            writerArray[i].start();
        }

        for (int i = 0; i < NUM_WRITERS; i++) {
            try {
                writerArray[i].join();
            }
            catch (InterruptedException ie) {
                fail();
            }
        }

        long endTime = System.currentTimeMillis();

        int window = (int) ((endTime - startTime) % Integer.MAX_VALUE);

        assertEquals(NUM_WRITERS * NUM_MSGS, tdq.getPeakLoad(window));
    }

}
