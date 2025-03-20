package hepker.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

class CustomDataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomDataManager.class);
    private static final int STRING_KEY_MAX_LEN = 100;
    private final CustomDatabase dataStore;

    private static int batchSize = 100;

    private DataNode head;
    private DataNode tail;

    private int listSize;

    CustomDataManager() {
        CustomDatabase tmpDb;
        try {
            tmpDb = new CustomDatabase();
            LOGGER.info("Initialized database successfully");
            this.listSize = 0;
            this.head = null;
            this.tail = null;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
        dataStore = tmpDb;
    }

    /**
     * Sets the exclusive quantity of nodes allowed to be queued for database storage. Default 100
     *
     * @param batchSize The exclusive maximum number of nodes to be stored in cache
     */
    static void setBatchSize(int batchSize) {
        CustomDataManager.batchSize = batchSize;
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
        if (++listSize >= batchSize) {
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
        if (key.length() > STRING_KEY_MAX_LEN) {
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
    private void pushData() {
        // push logic here
        head = null;
        tail = null;
        listSize = 0;
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
