package hepker.ai;

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
}
