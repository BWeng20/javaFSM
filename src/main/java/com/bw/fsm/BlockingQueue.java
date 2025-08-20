package com.bw.fsm;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * Wrapper around a java.util.concurrent.BlockingDeque, only here to simulate the "BlockingQueue" of the W3C Algorithm.
 */
public final class BlockingQueue<T> {

    public java.util.concurrent.BlockingDeque<T> data = new LinkedBlockingDeque<>();

    /***
     * <b>W3C says</b>:<br>
     * Puts e last in the queue
     */
    public void enqueue(T e) {
        data.add(e);
    }

    /***
     * <b>W3C says</b>:<br>
     * Removes and returns first element in queue, blocks if queue is empty
     */
    public T dequeue() {
        try {
            return data.takeFirst();
        } catch (InterruptedException ie) {
            return null;
        }
    }
}
