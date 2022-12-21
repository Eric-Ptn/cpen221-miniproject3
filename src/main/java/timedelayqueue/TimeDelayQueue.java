package timedelayqueue;

import java.util.*;
import java.sql.Timestamp;

// TODO: what is the thread safety argument?

/**
 * This data type stores PubSubMessage objects in a heap/priority queue,
 * ordered from the most recent to the oldest "processable message".
 * A message is processable when it has remained in
 * the TimeDelayQueue for at least the millisecondDelay parameter.
 */
public class TimeDelayQueue {
    private final int millisecondDelay; // delay before messages are processable
    private int msgProcessed; // total number of messages added

    private Queue<msgTimeInfo> delayQueue; // queue of messages, waiting for delay to be over
    private PriorityQueue<PubSubMessage> processQueue; // queue of processable messages, ordered by message timestamp
    private List<Timestamp> operations; // timestamps of operations (add or getNext)

    /**
     * Rep Invariant:
     *      millisecondDelay >= 0
     *      msgProcessed >= 0
     *
     * Abstraction Function:
     *      Represents a priority queue of messages based on timestamp, with delays before processing.
     *
     * Thread Safety:
     *      This class uses the 'synchronized' keyword on certain methods to
     *      avoid mistiming between users when multiple threads are running.
     *      See each method's thread safety on a case by case basis.
     *
     *      You will notice that in general, it is not critical that the methods
     *      are synchronized, as multiple instances of the TimeDelayQueue are
     *      by nature thread safe.
     */



    /**
     * Create a new TimeDelayQueue.
     * @param delay the delay, in milliseconds, that the queue can tolerate, >= 0
     */
    public TimeDelayQueue(int delay) {
        millisecondDelay = delay;
        msgProcessed = 0;
        processQueue = new PriorityQueue<>(new PubSubMessageComparator());
        delayQueue = new ArrayDeque<>();
        operations = new ArrayList<>();
    }

    /**
     * Adds a message to the TimeDelayQueue.
     * @param msg message to be added
     * @return true if the message is added, false
     *          if a message with the same id is already
     *          in queue
     */
    public synchronized boolean add(PubSubMessage msg) {
        if (inQueue(msg)) {
            return false;
        }
        Date date = new Date();
        delayQueue.add(new msgTimeInfo(msg, new Timestamp(date.getTime())));
        operations.add(new Timestamp(date.getTime()));
        msgProcessed++;

        return true;
    }

    /**
     * Get the count of the total number of messages processed
     * by this TimeDelayQueue.
     * @return total number of messages processed
     */
    public long getTotalMsgCount() {
        return msgProcessed;
    }

    /**
     * Get the next message and remove it from the queue.
     * @return the next message in the queue, if there is
     *          no available message returns PubSubMessage.NO_MSG
     */
    public synchronized PubSubMessage getNext() {
        Date date = new Date();
        operations.add(new Timestamp(date.getTime()));

        updateQueues();
        if (processQueue.isEmpty()) {
            return PubSubMessage.NO_MSG;
        }
        return processQueue.poll();
    }

    /**
     * Gets the maximum number of operations performed on this
     * TimeDelayQueue over a time interval timeWindow.
     * Only calls to add() and getNext() are counted as operations.
     * @param timeWindow length of time window, in milliseconds
     * @return the maximum number of operations that occurred
     *          within timeWindow milliseconds
     */
    public int getPeakLoad(int timeWindow) {
        int maxOperations = 1;
        for (int i = 0; i < operations.size() - 1; i++) {
            int currentOperations = 1;
            Timestamp startTime = operations.get(i);

            int j = i;
            Timestamp nextEndTime = operations.get(j + 1);
            while (j + 1 < operations.size() && nextEndTime.getTime() - startTime.getTime() <= timeWindow) {
                j++;
                currentOperations++;
                if (j == operations.size() - 1) { break; }
                nextEndTime = operations.get(j + 1);
            }

            maxOperations = Math.max(currentOperations, maxOperations);
        }
        return maxOperations;
    }

    /**
     * Moves messages from delayQueue into processQueue
     * if they are ready to be processed.
     */
    private void updateQueues() {
        Date date = new Date();
        msgTimeInfo currMsg = delayQueue.peek();
        while (!delayQueue.isEmpty() &&
                currMsg.time.getTime() + millisecondDelay < date.getTime()) {

            if (currMsg.msg.isTransient()) {
                TransientPubSubMessage transientMsg = (TransientPubSubMessage) currMsg.msg;
                 if (date.getTime() > currMsg.time.getTime() + transientMsg.getLifetime()){
                     delayQueue.poll();
                     currMsg = delayQueue.peek();
                     continue;
                 }
            }

            processQueue.add(currMsg.msg);
            delayQueue.poll();
            currMsg = delayQueue.peek();
        }
    }

    /**
     * Check if a message is present inside the TimeDelayQueue.
     * @param msgID ID of the message to check
     * @return true if the message is inside the queue, false otherwise
     */
    public boolean getMsg(UUID msgID) {
        return delayQueue.stream().anyMatch(x -> x.msg.getId().equals(msgID)) ||
                processQueue.stream().anyMatch(x -> x.getId().equals(msgID));
    }

    /**
     * Get the next message without removing it from the queue.
     * @return the next message in the queue, if there is
     *        no available message returns PubSubMessage.NO_MSG
     */
    public synchronized PubSubMessage peek() {
        updateQueues();
        if (processQueue.isEmpty()) {
            return PubSubMessage.NO_MSG;
        }
        return processQueue.peek();
    }

    /**
     * Checks if a message is in this TimeDelayQueue.
     * @param msg message to look for
     * @return true if the message is in the queue, false otherwise
     */
    private boolean inQueue(PubSubMessage msg) {
        return delayQueue.stream().anyMatch(x -> x.msg.equals(msg))
                || processQueue.contains(msg);
    }

    // a comparator to sort messages from earliest to latest
    private class PubSubMessageComparator implements Comparator<PubSubMessage> {
        public int compare(PubSubMessage msg1, PubSubMessage msg2) {
            return msg1.getTimestamp().compareTo(msg2.getTimestamp());
        }
    }

    /**
     * A class to store messages and the time they
     * were inserted into this TimeDelayQueue
     */
    class msgTimeInfo {
        PubSubMessage msg;
        Timestamp time; //the time at which msg was added to this TimeDelayQueue
        msgTimeInfo(PubSubMessage msg, Timestamp time) {
            this.msg = msg;
            this.time = time;
        }
    }
}
