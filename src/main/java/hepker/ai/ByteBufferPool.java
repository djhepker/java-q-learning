package hepker.ai;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Pools Bytebuffers for frequent reloading & loading of ByteBuffers
 */
final class ByteBufferPool {
    private final ConcurrentLinkedQueue<ByteBuffer> pool = new ConcurrentLinkedQueue<>();

    /**
     * Sets the number of ByteBuffers to be stored in pool
     *
     * @param initialSize Count of ByteBuffers needed by the program, allocated 1024 capacity to begin with
     */
    ByteBufferPool(int initialSize) {
        for (int i = 0; i < initialSize; i++) {
            pool.add(ByteBuffer.allocateDirect(1024));
        }
    }

    /**
     * Returns a ByteBuffer from pool with capacity 1024
     *
     * @return Pre-rendered ByteBuffer
     */
    ByteBuffer getBuffer() {
        return pool.poll() != null ? pool.poll() : ByteBuffer.allocateDirect(1024);
    }

    /**
     * Resets and places ByteBuffer back into the pool
     *
     * @param buffer Buffer to be reset and placed back into pooling for future use
     */
    void returnBuffer(ByteBuffer buffer) {
        buffer.clear();
        pool.add(buffer);
    }
}