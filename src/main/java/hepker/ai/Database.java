package hepker.ai;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/*
    We will use a header containing the indices of each String
    note that a String with only ascii characters, of length 95, is a maximum
    of 95 bytes

    File structure is as the following with no breaks:

    int m = long[].length   [ 4 bytes ]
    long[] where e : [0]key.length   [ 8 * m bytes ]

    Data Node:

    short Z = length of String key [ 2 bytes ]
    short X = number of doubles [ 2 bytes ]
    String key = stateKey   [ 100 bytes ] -> Allowed 100 printable Ascii in UTF-8
    double value    [ 8 * X bytes ]
*/

/**
 * Handles all write and read calls to .dat files
 */
final class Database {
    private final String dataFilePath = "src/main/resources/data/data.dat";
    private final AtomicBoolean isInitialized;
    private final ByteBufferPool bufferPool;

    private RandomAccessFile idxStore;
    private FileChannel idxChannel;

    /**
     * Constructor for initializing necessary dataStore management
     *
     * @throws IOException Failed to instantiate Database file
     */
    Database(ByteBufferPool bufferArg) throws IOException {
        this.isInitialized = new AtomicBoolean(false);
        this.bufferPool = bufferArg;
        initializeDatabase();
    }

    void writeData() throws IOException {

    }

    /**
     * Appends data to the end of the file
     *
     * @param byteSequence Represents Agent's state
     * @param channel Connection to the file that data will be appended to
     * @throws IOException If the connection is interrupted or there are file-related errors
     */
    void appendDataToFile(byte[] byteSequence, FileChannel channel) throws IOException {
        ByteBuffer bufferAppend = bufferPool.getBuffer();
        bufferAppend.put(byteSequence);
        channel.position(channel.size());
        while (bufferAppend.hasRemaining()) {
            channel.write(bufferAppend);
        }
        bufferPool.returnBuffer(bufferAppend);
    }

    /**
     * Scans a partition of .dat file for a specified value
     *
     * @throws IOException Thrown if interrupted while reading
     */
    double getValue(byte[] key, int valueIndex, long offset) throws IOException {
        // TODO: Test FileChannel pool to see if that improves performance
        try (FileChannel dataChannel = new RandomAccessFile(dataFilePath, "rw").getChannel()) {
            ByteBuffer dataBuffer = bufferPool.getBuffer();
            try {
                dataChannel.position(offset);
                dataChannel.read(dataBuffer);
                dataBuffer.flip();
                int storedKeyLength = dataBuffer.getShort();
                if (storedKeyLength != key.length) {
                    return -1.0;
                }
                int numValues = dataBuffer.getShort();
                if (valueIndex >= numValues) {
                    return 0.0;
                }

                byte[] byteLock = new byte[storedKeyLength];
                dataBuffer.get(byteLock);
                if (!Arrays.equals(byteLock, key)) {
                    return -1.0;
                }
                dataBuffer.position(dataBuffer.position() + valueIndex * 8);
            } finally {
                bufferPool.returnBuffer(dataBuffer);
            }
            return dataBuffer.getDouble();
        }
    }

    /**
     * Closes the database. Call once all reads and writes have been finalized
     */
    void close() throws IOException {
        idxStore.close();
        idxChannel.close();
    }

    private long getLockIndex(int keyLength, byte[] key) throws IOException {
        return 0;
    }

    /**
     * Reads byte data from the given file at the specified position
     *
     * @param channel   Channel used to read from the file
     * @param buffer    ByteBuffer to store the read data
     * @param initIndex  The position in the file to start reading from
     * @throws IOException Thrown if interrupted while reading
     * @return Int from file
     */
    int getDataInt(FileChannel channel, ByteBuffer buffer, long initIndex) throws IOException {
        channel.position(initIndex);
        channel.read(buffer);
        return buffer.getInt();
    }

    /**
     * Retrieves the number of long values stored in .idx as key indices
     *
     * @throws IOException Thrown if interrupted while reading
     * @return Number of long stored in .idx file
     */
    int getIdxInt(int offset) throws IOException {
        ByteBuffer indexBuffer = bufferPool.getBuffer();
        idxChannel.read(indexBuffer);
        int numIndices = indexBuffer.position(offset).getInt();
        bufferPool.returnBuffer(indexBuffer);
        return numIndices;
    }

    /**
     * Retrieves a long from .idx file
     *
     * @throws IOException Thrown if interrupted while reading
     * @return long representing offset of a String Key inside .dat
     */
    long getIdxLong(int offset) throws IOException {
        ByteBuffer longBuffer = bufferPool.getBuffer();
        idxChannel.read(longBuffer);
        long index = longBuffer.position(offset).getLong();
        bufferPool.returnBuffer(longBuffer);
        return index;
    }

    /**
     * Retrieves long[] of all indices stored in .idx
     *
     * @param numIndices The number of indices to retrieve
     * @return long[] of all stored indices
     * @throws IOException Connection error
     */
    long[] getIdxLongArray(int numIndices, int initPosition) throws IOException {
        int startPos = 4 + initPosition;
        int bytesToRead = numIndices * 8;
        long[] idxLongs = new long[numIndices];
        ByteBuffer longBuffer = bufferPool.getBuffer();
        try {
            if (longBuffer.capacity() < bytesToRead) {
                longBuffer = ByteBuffer.allocateDirect(bytesToRead);
            }
            idxChannel.position(startPos);
            idxChannel.read(longBuffer);
            longBuffer.flip();
            for (int i = 0; i < numIndices; ++i) {
                idxLongs[i] = longBuffer.getLong();
            }
        } finally {
            bufferPool.returnBuffer(longBuffer);
        }
        return idxLongs;
    }

    /**
     * Checks if database has been opened, opens if not
     */
    private void initializeDatabase() throws IOException {
        if (isInitialized.compareAndSet(false, true)) {
            initialize();
        }
    }

    /**
     * Renders the database, creating all necessary files
     */
    private void initialize() throws IOException {
        String dataDirectoryPath = "src/main/resources/data";
        if (!Files.exists(Paths.get(dataDirectoryPath))) {
            generateIdxFile(dataDirectoryPath);
        } else {
            this.idxStore = new RandomAccessFile(dataDirectoryPath + "/index.idx", "rw");
            this.idxChannel = idxStore.getChannel();
        }
    }

    /**
     * Generates all .dat files and creates an index header
     *
     * @param dataDirectoryPath Path where the files will be created
     * @throws IOException Database connection broken
     */
    private void generateIdxFile(String dataDirectoryPath) throws IOException {
        if (!new File(dataDirectoryPath).mkdirs()) {
            throw new IOException(String.format("Directory %s failed to initialize", dataDirectoryPath));
        }
        this.idxStore = new RandomAccessFile(dataDirectoryPath + "/index.idx", "rw");
        this.idxChannel = idxStore.getChannel();

        // int numkeys, int numInvalid, long[32] (indices)
        int initialIndices = 32;
        ByteBuffer header = bufferPool.getBuffer();
        header.putInt(0);
        header.putInt(0);
        for (int i = 0; i < initialIndices; i++) {
            header.putLong(0);
        }
        header.flip();
        appendDataToFile(header.array(), idxStore.getChannel());

        bufferPool.returnBuffer(header);
    }
}
