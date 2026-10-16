package org.teavm.classlib.java.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Executors in the browser build run each task on the caller, at once. The game only uses pools
 * for background work it then waits for, so this keeps its results and order.
 */
public abstract class TAbstractExecutorService implements TExecutorService {
    boolean shutdown;

    @Override
    public void execute(org.teavm.classlib.java.lang.TRunnable command) {
        if (shutdown) {
            throw new TRejectedExecutionException();
        }
        try {
            command.run();
        } catch (Throwable t) {
            Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
            if (handler != null) {
                handler.uncaughtException(Thread.currentThread(), t);
            } else {
                t.printStackTrace();
            }
        }
    }

    @Override
    public <T> TFuture<T> submit(java.util.concurrent.Callable<T> task) {
        if (shutdown) {
            throw new TRejectedExecutionException();
        }
        return TCompletedFuture.run(task);
    }

    @Override
    public <T> TFuture<T> submit(Runnable task, T result) {
        java.util.concurrent.Callable<T> callable = () -> {
            task.run();
            return result;
        };
        return submit(callable);
    }

    @Override
    public TFuture<?> submit(Runnable task) {
        return submit(task, null);
    }

    @Override
    public <T> List<TFuture<T>> invokeAll(Collection<? extends java.util.concurrent.Callable<T>> tasks) {
        List<TFuture<T>> out = new ArrayList<>();
        for (java.util.concurrent.Callable<T> task : tasks) {
            out.add(submit(task));
        }
        return out;
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown = true;
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return shutdown;
    }

    @Override
    public boolean awaitTermination(long timeout, TTimeUnit unit) {
        return true;
    }
}
