package hepker.ai;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ByteBufferPool {
    private final ConcurrentLinkedQueue<ByteBuffer> pool = new ConcurrentLinkedQueue<>();

    public ByteBufferPool(int initialSize) {
        for (int i = 0; i < initialSize; i++) {
            pool.add(ByteBuffer.allocateDirect(1024));
        }
    }

    public ByteBuffer getBuffer() {
        return pool.poll() != null ? pool.poll() : ByteBuffer.allocateDirect(1024);
    }

    public void returnBuffer(ByteBuffer buffer) {
        buffer.clear();
        pool.add(buffer);
    }
}