package org.teavm.classlib.java.util.concurrent;

public final class TExecutors {
    private TExecutors() {
    }

    static class Direct extends TAbstractExecutorService implements TScheduledExecutorService {
    }

    public static TExecutorService newFixedThreadPool(int threads) {
        return new Direct();
    }

    public static TExecutorService newFixedThreadPool(int threads, TThreadFactory factory) {
        return new Direct();
    }

    public static TExecutorService newSingleThreadExecutor() {
        return new Direct();
    }

    public static TExecutorService newSingleThreadExecutor(TThreadFactory factory) {
        return new Direct();
    }

    public static TExecutorService newCachedThreadPool() {
        return new Direct();
    }

    public static TExecutorService newCachedThreadPool(TThreadFactory factory) {
        return new Direct();
    }

    public static TScheduledExecutorService newScheduledThreadPool(int threads) {
        return new Direct();
    }

    public static TExecutorService newWorkStealingPool() {
        return new Direct();
    }
}
