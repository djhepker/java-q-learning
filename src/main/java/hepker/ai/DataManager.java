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
    private static int batchSize = 12_000;

    private final DataBridge bridge;
    private final DataArray dataArray;

    /**
     * Safely constructs creation of the DataBridge and corresponding Database
     */
    DataManager() {
        this.dataArray = new DataArray(batchSize);
        DataBridge tmpB;
        try {
            tmpB = new DataBridge();
            LOGGER.info("Initialized Database Successfully");
        } catch (Exception e) {
            String errorMessage = "DataManager Failed to Initialize Database";
            LOGGER.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
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
        try {
            return bridge.getValue(key.getBytes(), actionIndex);
        } catch (IOException | InterruptedException e) {
            LOGGER.error(String.format("Failed to get value for key %s with index %d", key, actionIndex), e);
            throw new RuntimeException(e);
        }
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
        return dataArray.getCacheSize();
    }

    /**
     * Sets the exclusive quantity of nodes allowed to be queued for database storage. Default 100
     *
     * @param newBatchSize The exclusive maximum number of nodes to be stored in cache
     */
    void setBatchSize(int newBatchSize) {
        batchSize = newBatchSize;
    }

    /**
     * Queues up data for addition to the database. Stored as a singly linked-list. String stateKey must not exceed
     * a length of 100 characters. Will throw a RuntimeException in such a case
     *
     * @param stateKey String representation of world state
     * @param actionIndex Chosen action in given world state
     * @param value Value of making action in state stateKey
     */
    void queueData(String stateKey, int actionIndex, double value) {
        byte[] dataArr = convertTupleToBytes((short) actionIndex, stateKey, value);
        dataArray.queueData(dataArr);
        if (dataArray.getCacheSize() >= batchSize) {
            pushData();
        }
    }

    /**
     * Converts data to a byte[]. keylength, stateKey, action index, value, in that order
     *
     * @param actionIndex Chosen action in given world state
     * @param stateKey String representation of world state
     * @param value Value of making action in state stateKey
     * @return byte[] of input arguments in order of
     */
    private byte[] convertTupleToBytes(short actionIndex, String stateKey, double value) {
        if (stateKey.length() > 100) {
            throw new RuntimeException(String.format("StateKey is too long: %s", stateKey.length()));
        }
        byte[] keyBytes = stateKey.getBytes(StandardCharsets.UTF_8);
        short keyLength = (short) keyBytes.length;
        ByteBuffer byteBuffer = ByteBuffer.allocate(12 + keyLength);
        byteBuffer.putShort(keyLength); // Checked first in query; fastest way to determine valid stateKey
        byteBuffer.put(keyBytes); // key is checked second to verify search success
        byteBuffer.putShort(actionIndex); // Allows query check if desired index exists
        byteBuffer.putDouble(value);

        return byteBuffer.array();
    }

    /**
     * Pushes all queued data to the database, eliminating cached bytes
     */
    void pushData() {
        try {
            bridge.writeCache(dataArray.getCachedBytes(), dataArray.getCacheIndices());
        } catch (Exception e) {
            String errorMessage = "Failed to Push Data";
            LOGGER.error(errorMessage, e);
        }
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
     * Array object for storing bytes to be written to Database
     */
    private class DataArray {
        private byte[] data;
        private int[] cacheIndex;
        private int cacheSize = 0;
        private int numIndices = 0;

        DataArray(int initialCapacity) {
            data = new byte[initialCapacity];
            cacheIndex = new int[initialCapacity / 100];
        }

        /**
         * Adds bytes to the data[] for writing
         *
         * @param byteSequence Bytes to be written to .dat file
         */
        void queueData(byte[] byteSequence) {
            int cacheSnapshot = cacheSize;
            int sequenceLength = byteSequence.length;
            cacheSize += sequenceLength;
            if (cacheSize >= data.length) {
                byte[] newData = new byte[data.length * 2];
                System.arraycopy(data, 0, newData, 0, cacheSnapshot);
                data = newData;
            }
            if (++numIndices >= cacheIndex.length) {
                int[] newCacheIndex = new int[cacheIndex.length * 2];
                System.arraycopy(cacheIndex, 0, newCacheIndex, 0, cacheIndex.length);
                cacheIndex = newCacheIndex;
            }
            System.arraycopy(byteSequence, 0, data, cacheSnapshot, sequenceLength);
            cacheIndex[numIndices] = sequenceLength; // first index is obviously zero
        }

        /**
         * Retrieves the number of bytes currently queued as data
         *
         * @return count of bytes stored
         */
        int getCacheSize() {
            return cacheSize;
        }

        /**
         * Getter for cache indices
         *
         * @return int[] of cache indices
         */
        int[] getCacheIndices() {
            int[] exportIndices = new int[numIndices];
            System.arraycopy(cacheIndex, 0, exportIndices, 0, numIndices);
            data = new byte[batchSize / 100];
            numIndices = 0;
            return exportIndices;
        }

        /**
         * Retrieves and empties cash reserves of DataArray. member byte[] is emptied, reset to batchSize
         *
         * @return bytes queued to be written to .dat file
         */
        byte[] getCachedBytes() {
            byte[] exportBytes = new byte[cacheSize];
            System.arraycopy(data, 0, exportBytes, 0, cacheSize);
            data = new byte[batchSize];
            cacheSize = 0;
            return exportBytes;
        }
    }
}
