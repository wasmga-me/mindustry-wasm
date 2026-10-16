package org.teavm.classlib.java.util.concurrent;

import java.util.Collection;
import java.util.List;

public interface TExecutorService extends TExecutor, AutoCloseable {
    void shutdown();

    List<Runnable> shutdownNow();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TTimeUnit unit) throws InterruptedException;

    <T> TFuture<T> submit(java.util.concurrent.Callable<T> task);

    <T> TFuture<T> submit(Runnable task, T result);

    TFuture<?> submit(Runnable task);

    <T> List<TFuture<T>> invokeAll(Collection<? extends java.util.concurrent.Callable<T>> tasks) throws InterruptedException;

    @Override
    default void close() {
        shutdown();
    }
}
