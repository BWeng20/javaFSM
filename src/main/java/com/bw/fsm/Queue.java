package com.bw.fsm;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Wrapper around a java.util.Deque, simulate the "Queue" of the W3C Algorithm -
 * including some extensions needed.
 */
public class Queue<T> {

    public Deque<T> data = new ArrayDeque<>();

    /**
     * Extension to re-use exiting instances.
     */
    public void clear() {
        this.data.clear();
    }

    /**
     * <b>W3C says</b>:<br>
     * Puts e last in the queue
     */
    public void enqueue(T e) {
        this.data.add(e);
    }

    /**
     * <b>W3C says</b>:<br>
     * Removes and returns first element in queue
     */
    public T dequeue() {
        return this.data.pop();
    }

    /**
     * <b>W3C says</b>:<br>
     * Is the queue empty?
     */
    public boolean isEmpty() {
        return this.data.isEmpty();
    }
}
