package org.teavm.classlib.java.util.concurrent;

public class TLinkedBlockingQueue<E> extends TArrayBlockingQueue<E> {
    public TLinkedBlockingQueue() {
        super(1 << 16);
    }
}
