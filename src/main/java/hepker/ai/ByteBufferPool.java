package hepker.ai;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

final class ByteBufferPool {
    private final ConcurrentLinkedQueue<ByteBuffer> pool = new ConcurrentLinkedQueue<>();

    ByteBufferPool(int initialSize) {
        for (int i = 0; i < initialSize; i++) {
            pool.add(ByteBuffer.allocateDirect(1024));
        }
    }

    ByteBuffer getBuffer() {
        return pool.poll() != null ? pool.poll() : ByteBuffer.allocateDirect(1024);
    }

    void returnBuffer(ByteBuffer buffer) {
        buffer.clear();
        pool.add(buffer);
    }
}