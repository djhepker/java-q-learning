package hepker.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

final class BridgeUtilities {
    private final Database datastore;

    BridgeUtilities(Database storeArg) {
        this.datastore = storeArg;
    }

    void searchTask(byte[] targetKey, int valueIndex,
                    long[] offsets, AtomicReference<Double> result,
                    CountDownLatch latch) {
        try {
            for (long offset : offsets) {
                if (result.get() != null) {
                    return;
                }
                //TODO figure out how to deal with missing values better than -1, clearly won't work
                double queryResult = datastore.getValue(targetKey, valueIndex, offset);
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

    /**
     * Delegates Bytes into [] representing individual Data writes
     *
     * @param dataSequence cache[] of data to be written to file
     * @param dataIndices indices separating Data writes
     * @return List of byte[] to be written to file
     */
    List<byte[]> getByteDelegation(byte[] dataSequence, int[] dataIndices) {
        List<byte[]> results = new ArrayList<>();
        int sequencePosition = 0;
        for (int index : dataIndices) { // First index of dataIndices = zero, pointing to the start of dataSequence
            sequencePosition += index; // Saves position in sequence via reference
            byte[] resultElement = new byte[index];
            System.arraycopy(dataSequence, sequencePosition, resultElement, 0, index);
            results.add(resultElement);
        }
        return results;
    }


    /**
     * Divides offsets into relatively equivalent batches
     *
     * @param offsets Indices of stateKey in .dat
     * @param threadPoolSize Number of threads we are using to parallel task
     * @return Container of roughly equivalent sized offsets
     */
    List<long[]> getLongDelegation(long[] offsets, int threadPoolSize) {
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
