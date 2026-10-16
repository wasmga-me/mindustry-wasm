package org.teavm.classlib.java.util.concurrent;

public class TSynchronousQueue<E> extends TArrayBlockingQueue<E> {
    public TSynchronousQueue() {
        super(1);
    }
}
