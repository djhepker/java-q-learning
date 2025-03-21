package hepker.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Manager employed by Agent to handle learned information values. Middleman between Agent and Database
 */
final class DataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataManager.class);
    private static int batchSize = 100;

    private final DataBridge bridge;
    private DataNode head;
    private DataNode tail;

    private int cacheSize;

    /**
     * Safely constructs creation of the DataBridge and corresponding Database
     */
    DataManager() {
        DataBridge tmpB;
        try {
            tmpB = new DataBridge();
            LOGGER.info("Initialized database successfully");
            this.cacheSize = 0;
            this.head = null;
            this.tail = null;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
        bridge = tmpB;
    }

    /**
     * Queries database for the given state-action pair
     *
     * @param key String representation of Agent's state
     * @param actionIndex Index of the action taken by Agent
     * @return Value of performing action actionIndex in state key. Returns 0.0 if not found
     */
    double queryValue(String key, int actionIndex) {
        if (true) {
            // retrieve through bridge logic, return value
        }
        return 0.0;
    }

    /**
     * Queries and retrieves the maximum Q-value given a String representation of Agent's state
     *
     * @param key String representation of Agent's state
     * @return The maximum held Q-value of the state
     */
    double getMaxQValue(String key) {
        // retrieve through bridge logic
        return 0.0;
    }

    /**
     * Queries and retrieves action index of the maximum Q-value given a String representation of Agent's state
     *
     * @param key String representation of Agent's state
     * @return Index of the maximum held Q-value of the state
     */
    int getMaxQIndex(String key) {
       // retrieve through bridge logic
        return 0;
    }

    /**
     * Retrieves the number of DataNodes stored in memory. Size is a member variable int, adjusted with
     * all list operations
     *
     * @return cacheSize Counter of DataNodes being tracked
     */
    int getCacheSize() {
        return cacheSize;
    }

    /**
     * Sets the exclusive quantity of nodes allowed to be queued for database storage. Default 100
     *
     * @param batchSize The exclusive maximum number of nodes to be stored in cache
     */
    void setBatchSize(int batchSize) {
        DataManager.batchSize = batchSize;
    }

    /**
     * Queues up data for addition to the database. Stored as a singly linked-list. String key must not exceed
     * a length of 100 characters. Will throw a RuntimeException in such a case
     *
     * @param key String representation of world state
     * @param actionIndex Chosen action in given world state
     * @param value Value of making action in state key
     */
    void queueData(String key, int actionIndex, double value) {
        byte[] dataArr = convertTupleToBytes(actionIndex, key, value);

        if (head != null) {
            tail = new DataNode(dataArr, tail);
        } else {
            head = new DataNode(dataArr, null);
            tail = head;
        }
        if (++cacheSize >= batchSize) {
            pushData();
        }
    }

    /**
     * Converts data to a byte[]. keylength, key, action index, value
     *
     * @param actionIndex Chosen action in given world state
     * @param key String representation of world state
     * @param value Value of making action in state key
     * @return byte[] of input arguments in order of
     */
    private byte[] convertTupleToBytes(int actionIndex, String key, double value) {
        if (key.length() > 100) {
            throw new RuntimeException(String.format("StateKey is too long: %s", key.length()));
        }
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        short keyLength = (short) keyBytes.length;
        ByteBuffer byteBuffer = ByteBuffer.allocate(2 + keyLength + 4 + 8);
        byteBuffer.putShort(keyLength);
        byteBuffer.put(keyBytes);
        byteBuffer.putInt(actionIndex);
        byteBuffer.putDouble(value);

        return byteBuffer.array();
    }

    /**
     * Pushes all queued data to the database, eliminating cached nodes
     *
     */
    void pushData() {
        // push logic here
        head = null;
        tail = null;
        cacheSize = 0;
    }

    /**
     * Closes db once finalized
     */
    void close() {
        try {
            bridge.close();
        } catch (IOException e) {
            LOGGER.error("Failed to close database", e);
        }
    }

    /**
     * Node in singly linked-list which contains a statekey, index action, value, and a link
     * to the next node in the list
     *
     * @param dataBytes byte[] of the data to be stored in the db
     * @param next The next DataNode. Null if this node is the head node
     */
    record DataNode(byte[] dataBytes, DataNode next) {

    }
}
