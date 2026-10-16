package org.teavm.classlib.java.util.concurrent;

public class TForkJoinPool extends TAbstractExecutorService {
    private static final TForkJoinPool common = new TForkJoinPool();

    public static TForkJoinPool commonPool() {
        return common;
    }

    public <T> T invoke(TForkJoinTask<T> task) {
        return task.invoke();
    }

    public void execute(TForkJoinTask<?> task) {
        task.invoke();
    }

    @Override
    public TForkJoinTask<?> submit(Runnable task) {
        TForkJoinTask<Void> wrapped = new TRecursiveAction() {
            @Override
            protected void compute() {
                task.run();
            }
        };
        wrapped.invoke();
        return wrapped;
    }

    public <T> TForkJoinTask<T> submit(TForkJoinTask<T> task) {
        task.invoke();
        return task;
    }
}
