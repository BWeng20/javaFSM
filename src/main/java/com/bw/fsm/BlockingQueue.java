package com.bw.fsm;

import java.util.concurrent.LinkedBlockingDeque;

public class BlockingQueue<T> {

    public java.util.concurrent.BlockingDeque<T> data = new LinkedBlockingDeque<>();

    /// *W3C says*:
    /// Puts e last in the queue
    public void enqueue(T e) {
        data.add(e);
    }

    /// *W3C says*:
    /// Removes and returns first element in queue, blocks if queue is empty
    public T dequeue() {
        try {
            return data.takeFirst();
        } catch (InterruptedException ie) {
            return null;
        }
    }
}
