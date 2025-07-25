package com.bw.fsm;

import java.util.ArrayDeque;
import java.util.Deque;

/// Queue datatype used by the algorithm
public class Queue<T> {

    public Deque<T> data = new ArrayDeque<>();


    /// Extension to re-use exiting instances.
    public void clear() {
        this.data.clear();
    }

    /// *W3C says*:
    /// Puts e last in the queue
    public void enqueue(T e) {
        this.data.add(e);
    }

    /// *W3C says*:
    /// Removes and returns first element in queue
    public T dequeue() {
        return this.data.pop();
    }

    /// *W3C says*:
    /// Is the queue empty?
    public boolean isEmpty() {
        return this.data.isEmpty();
    }
}
