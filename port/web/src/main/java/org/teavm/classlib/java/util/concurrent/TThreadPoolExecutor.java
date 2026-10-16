package org.teavm.classlib.java.util.concurrent;

public class TThreadPoolExecutor extends TAbstractExecutorService {
    public TThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TTimeUnit unit,
            TBlockingQueue<Runnable> workQueue) {
    }

    public TThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TTimeUnit unit,
            TBlockingQueue<Runnable> workQueue, TThreadFactory threadFactory) {
    }

    public void allowCoreThreadTimeOut(boolean value) {
    }

    public int getActiveCount() {
        return 0;
    }
}
