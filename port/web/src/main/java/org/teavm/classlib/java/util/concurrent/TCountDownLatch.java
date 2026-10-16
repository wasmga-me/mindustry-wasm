package org.teavm.classlib.java.util.concurrent;

public class TCountDownLatch {
    private long count;

    public TCountDownLatch(int count) {
        this.count = count;
    }

    public synchronized void countDown() {
        if (count > 0 && --count == 0) {
            notifyAll();
        }
    }

    public long getCount() {
        return count;
    }

    public synchronized void await() throws InterruptedException {
        while (count > 0) {
            wait();
        }
    }

    public synchronized boolean await(long timeout, TTimeUnit unit) throws InterruptedException {
        if (count > 0) {
            wait(Math.max(1, unit.toMillis(timeout)));
        }
        return count == 0;
    }
}
