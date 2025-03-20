package hepker.ai;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
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
class Database {
    private static final int INT_BYTES = 4;
    private static final int DOUBLE_LONG_BYTES = 8;

    private final AtomicBoolean isInitialized;
    private final ByteBufferPool bufferPool;

    private RandomAccessFile dataStore;
    private FileChannel dataChannel;
    private RandomAccessFile indexStore;
    private FileChannel indexChannel;


    /**
     * Constructor for initializing necessary dataStore management
     *
     * @throws IOException Failed to instantiate Database file
     */
    Database() throws IOException {
        this.isInitialized = new AtomicBoolean(false);
        this.bufferPool = new ByteBufferPool(10);
        initializeDatabase();
    }

    /**
     * Writes byte data to the given file starting at the specified position.
     *
     * @param dataBuffer      ByteBuffer of data to be written
     * @param channel         Channel used to write to the file
     * @param initIndex The position in the file to start writing from
     * @throws IOException Thrown if interrupted while writing
     */
    void writeData(ByteBuffer dataBuffer, FileChannel channel, long initIndex) throws IOException {
        channel.position(initIndex); // Set position to specified index
        dataBuffer.flip();
        while (dataBuffer.hasRemaining()) {
            channel.write(dataBuffer);
        }
    }

    /**
     * Reads byte data from the given file at the specified position
     *
     * @throws IOException Thrown if interrupted while reading
     */
    double getValue(byte[] key, int actionIndex) throws IOException {
        int keyLength = key.length;
        int numIndicies = getDataInt(indexChannel, ByteBuffer.allocate(4), 0);
        long lockIndex = getLockIndex(keyLength, key);

        return 0.0;
    }

    /**
     * Closes the database. Call once all reads and writes have been finalized
     */
    void close() throws IOException {
        dataStore.close();
        indexStore.close();
        dataChannel.close();
        indexChannel.close();
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
     */
    private double getDataDouble(FileChannel channel, ByteBuffer buffer, long initIndex) throws IOException {

        return 0.0;
    }

    /**
     * Reads byte data from the given file at the specified position
     *
     * @param channel   Channel used to read from the file
     * @param buffer    ByteBuffer to store the read data
     * @param initIndex  The position in the file to start reading from
     * @throws IOException Thrown if interrupted while reading
     */
    private int getDataInt(FileChannel channel, ByteBuffer buffer, long initIndex) throws IOException {
        channel.position(initIndex);
        channel.read(buffer);
        return buffer.getInt();
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
            generateDataIndices(dataDirectoryPath);
        } else {
            this.indexStore = new RandomAccessFile(dataDirectoryPath + "/index_values.dat", "rw");
            this.dataStore = new RandomAccessFile(dataDirectoryPath + "/q_values.dat", "rw");
            this.indexChannel = indexStore.getChannel();
            this.dataChannel = dataStore.getChannel();
        }
    }

    /**
     * Generates all .dat files and creates an index header
     *
     * @param dataDirectoryPath Path where the files will be created
     * @throws IOException Database connection broken
     */
    private void generateDataIndices(String dataDirectoryPath) throws IOException {
        if (!new File(dataDirectoryPath).mkdirs()) {
            throw new IOException(String.format("Directory %s failed to initialize", dataDirectoryPath));
        }
        this.indexStore = new RandomAccessFile(dataDirectoryPath + "/index_values.dat", "rw");
        this.dataStore = new RandomAccessFile(dataDirectoryPath + "/q_values.dat", "rw");
        this.indexChannel = indexStore.getChannel();
        this.dataChannel = dataStore.getChannel();

        // int numkeys, int numInvalid, long[32] (indices)
        int initialIndices = 32;
        ByteBuffer header = bufferPool.getBuffer();
        header.putInt(0);
        header.putInt(0);
        for (int i = 0; i < initialIndices; i++) {
            header.putLong(0);
        }
        header.flip();
        writeData(header, indexChannel, 0);
        bufferPool.returnBuffer(header);
    }
}
