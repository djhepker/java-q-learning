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
    private ExecutorService threadPool;

    DataBridge(ByteBufferPool bufferArg) throws IOException {
        this.dataStore = new Database(bufferArg);
        this.threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    void writeValue() {
        //dataStore.writeData()
    }

    /**
     * Reads byte data from the given file at the specified position
     *
     * @throws IOException Thrown if interrupted while reading
     */
    double getValue(byte[] key, int actionIndex) throws IOException, InterruptedException {
        int numIndices = dataStore.getIdxInt(0);
        int threadPoolSize = Runtime.getRuntime().availableProcessors();

        AtomicReference<Double> result = new AtomicReference<>(null);

        // TODO: test if it is faster to divide offsetArr in half and start one thread at each end
        long[] offsetArr = dataStore.getIdxLongArray(numIndices, 0);

        List<long[]> batches = divideIntoBatches(offsetArr, threadPoolSize);
        CountDownLatch latch = new CountDownLatch(batches.size());
        for (long[] batch : batches) {
            threadPool.submit(() -> {
                try {
                    new SearchTask(key, actionIndex, batch, result, latch).process();
                } catch (IOException e) {
                    throw new RuntimeException("Error while parallel processing SearchTask", e);
                }
            });
        }
        latch.await();
        return result.get() == null ? 0.0 : result.get();
    }

    private long queryDB() {
        return 0L;
    }

    private List<long[]> divideIntoBatches(long[] indices, int threadPoolSize) {
        List<long[]> batches = new ArrayList<>();
        int numIndices = indices.length;
        int batchSize = (numIndices + threadPoolSize - 1) / threadPoolSize;

        for (int i = 0; i < numIndices; i += batchSize) {
            int end = Math.min(i + batchSize, numIndices);
            batches.add(Arrays.copyOfRange(indices, i, end));
        }
        return batches;
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

    private class SearchTask {
        private final long[] offsets;
        private final byte[] targetKey;
        private final AtomicReference<Double> result;
        private final CountDownLatch latch;
        private final int valueIndex;

        SearchTask(byte[] targetKey, int valueIndex,
                   long[] offsets, AtomicReference<Double> result, CountDownLatch latch) {
            this.targetKey = targetKey;
            this.result = result;
            this.latch = latch;
            this.offsets = offsets;
            this.valueIndex = valueIndex;
        }

        void process() throws IOException {
            try {
                for (long offset : offsets) {
                    if (result.get() != null) {
                        return;
                    }
                    //TODO figure out how to deal with missing values better than -1, clearly won't work
                    double queryResult = dataStore.getValue(targetKey, valueIndex, offset);
                    if (queryResult >= 0) {
                        result.set(queryResult);
                    }
                }
            } catch (Exception e) {
              String errorMessage = "Error while parallel processing SearchTask.";
              throw new RuntimeException(errorMessage, e);
            } finally {
                latch.countDown();
            }
        }
    }
}
