package hepker.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
     * @param bufferArg BufferPool to be used by Database
     * @throws IOException If the connections are interrupted
     */
    DataBridge(ByteBufferPool bufferArg) throws IOException {
        this.dataStore = new Database(bufferArg);
        this.dataUtils = new BridgeUtilities(dataStore);
        this.threadPoolSize = Runtime.getRuntime().availableProcessors();
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
    }

    /**
     * Handles parallel processing for writing (keylength, stateKey, action index, value, in that order)
     *
     * @param dataSequence Queued data to be written to .dat file
     * @throws IOException If an I/O error occurs
     */
    void writeValue(byte[] dataSequence) throws IOException {
        //TODO parallel delegation of the write
        long[] offsets = dataStore.getIdxLongArray(dataStore.getIdxInt(0), 0);

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
        List<long[]> batches = divideIntoBatches(offsetArr, threadPoolSize);
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

    /**
     * Divides offsets into relatively equivalent batches
     *
     * @param offsets Indices of stateKey in .dat
     * @param threadPoolSize Number of threads we are using to parallel task
     * @return Container of roughly equivalent sized offsets
     */
    private List<long[]> divideIntoBatches(long[] offsets, int threadPoolSize) {
        List<long[]> batches = new ArrayList<>();
        int numIndices = offsets.length;
        int batchSize = (numIndices + threadPoolSize - 1) / threadPoolSize;

        for (int i = 0; i < numIndices; i += batchSize) {
            int end = Math.min(i + batchSize, numIndices);
            batches.add(Arrays.copyOfRange(offsets, i, end));
        }
        return batches;
    }
}
