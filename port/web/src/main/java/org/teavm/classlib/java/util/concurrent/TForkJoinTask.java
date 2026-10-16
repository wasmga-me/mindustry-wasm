package org.teavm.classlib.java.util.concurrent;

/** Runs on the caller, like the other browser executors. */
public abstract class TForkJoinTask<V> implements TFuture<V> {
    private boolean done;

    protected abstract boolean exec();

    public abstract V getRawResult();

    protected abstract void setRawResult(V value);

    public final V invoke() {
        if (!done) {
            exec();
            done = true;
        }
        return getRawResult();
    }

    public final TForkJoinTask<V> fork() {
        invoke();
        return this;
    }

    public final V join() {
        return invoke();
    }

    public static void invokeAll(TForkJoinTask<?> a, TForkJoinTask<?> b) {
        a.invoke();
        b.invoke();
    }

    public static void invokeAll(TForkJoinTask<?>... tasks) {
        for (TForkJoinTask<?> task : tasks) {
            task.invoke();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public V get() {
        return invoke();
    }

    @Override
    public V get(long timeout, TTimeUnit unit) {
        return invoke();
    }
}
