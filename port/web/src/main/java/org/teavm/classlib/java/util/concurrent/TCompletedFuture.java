package org.teavm.classlib.java.util.concurrent;

/** Result of a task that already ran: the browser build runs pool tasks on the caller. */
class TCompletedFuture<V> implements TFuture<V> {
    private final V value;
    private final Throwable error;

    TCompletedFuture(V value, Throwable error) {
        this.value = value;
        this.error = error;
    }

    static <V> TCompletedFuture<V> run(java.util.concurrent.Callable<V> task) {
        try {
            return new TCompletedFuture<>(task.call(), null);
        } catch (Throwable t) {
            return new TCompletedFuture<>(null, t);
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
        return true;
    }

    @Override
    public V get() throws TExecutionException {
        if (error != null) {
            throw new TExecutionException(error);
        }
        return value;
    }

    @Override
    public V get(long timeout, TTimeUnit unit) throws TExecutionException {
        return get();
    }
}
