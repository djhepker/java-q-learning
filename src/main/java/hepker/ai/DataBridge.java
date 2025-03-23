package hepker.ai;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles threading calls between DataManager and Database
 */
final class DataBridge {
    private final Database dataStore;
    private final ExecutorService threadPool;
    private final BridgeUtilities dataUtils;
    private final int threadPoolSize;

    /**
     * Constructs final variables and passes through ByteBufferPool
     *
     * @throws IOException If the connections are interrupted
     */
    DataBridge() throws IOException {
        this.dataStore = new Database();
        this.dataUtils = new BridgeUtilities(dataStore);
        this.threadPoolSize = Runtime.getRuntime().availableProcessors();
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
    }

    /**
     * Handles parallel processing for writing (keylength, stateKey, action index, value, in that order)<br>
     * .idx is [int numKeys][int numInvalid][short keySize][long keyIndex]
     *
     * @param dataSequence Queued data to be written to .dat file
     * @throws IOException If an I/O error occurs
     */
    void writeCache(byte[] dataSequence, int[] dataIndices) throws IOException, InterruptedException {
        long[] offsetArr = dataStore.getIdxLongArray(dataStore.getIdxInt(0), 0);
        //AtomicReference<Long> value = new AtomicReference<>();
        List<long[]> batches = dataUtils.getLongDelegation(offsetArr, threadPoolSize);
        CountDownLatch latch = new CountDownLatch(batches.size());

        for (long[] batch : batches) {
            threadPool.submit(() -> {
                dataUtils.writeToDatabase(dataSequence);
            });
        }

        latch.await();

        dataStore.writeData();
    }

    /**
     * Retrieves a value from the database using parallel processing across multiple threads.
     * Each thread processes a batch of offsets to search for the key.
     *
     * @param key The key to search for
     * @param actionIndex The index of the value to retrieve
     * @return The value associated with the key, or 0.0 if not found
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the thread is interrupted
     */
    double getValue(byte[] key, int actionIndex) throws IOException, InterruptedException, RuntimeException {
        int numIndices = dataStore.getIdxInt(0);
        AtomicReference<Double> result = new AtomicReference<>(null);
        // TODO: test if it is faster to divide offsetArr in half and start one thread at each end
        long[] offsetArr = dataStore.getIdxLongArray(numIndices, 0);
        List<long[]> batches = dataUtils.getLongDelegation(offsetArr, threadPoolSize);
        CountDownLatch latch = new CountDownLatch(batches.size());
        for (long[] batch : batches) {
            threadPool.submit(() -> {
                dataUtils.searchTask(key, actionIndex, batch, result, latch);
            });
        }
        latch.await();
        return result.get() == null ? 0.0 : result.get();
    }

    /**
     * Safely closes Database
     *
     * @throws IOException Throws if database failed to close properly
     */
    void close() throws IOException {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        dataStore.close();
    }

    private long queryDB() {
        return 0L;
    }
}
