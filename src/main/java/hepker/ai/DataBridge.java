package hepker.ai;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Handles calls between DataManager and Database
 */
final class DataBridge {
    private Database db;

    DataBridge() {

    }

    /**
     * Reads byte data from the given file at the specified position
     *
     * @throws IOException Thrown if interrupted while reading
     */
    double getValue(byte[] key, int actionIndex) throws IOException {
        int keyLength = key.length;
        int numIndicies = db.getDataInt(indexChannel, ByteBuffer.allocate(4), 0);
        long lockIndex = db.getLockIndex(keyLength, key);

        return 0.0;
    }
}
