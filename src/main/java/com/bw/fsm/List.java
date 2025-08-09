package com.bw.fsm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Predicate;

/// ## General Purpose List type, as used in the W3C algorithm.
public class List<E> {
    public final java.util.List<E> data;

    public List() {
        this.data = new ArrayList<>();
    }


    public List(java.util.List<E> data) {
        this.data = data;
    }

    /// Extension to create a list from an array.
    public static <E> List<E> from_array(E[] l) {
        return new List<>(new ArrayList<>(Arrays.asList(l)));
    }

    /// Extension to return the current size of the list.
    public int size() {
        return data.size();
    }

    /// Extension to add an element at the end of the list.
    public void push(E e) {
        data.add(e);
    }

    /// Extension to merge the specified list into this list.
    public void push_set(OrderedSet<E> l) {
        this.data.addAll(l.data);
    }

    /// *W3C says:* Returns the head of the list
    public E head() {
        return this.data.isEmpty() ? null : this.data.get(0);
    }

    /// *W3C says*:
    /// Returns the tail of the list (i.e., the rest of the list once the head is removed)
    public List<E> tail() {
        List<E> t = new List<>(new ArrayList<>(this.data));
        t.data.remove(0);
        return t;
    }

    /// *W3C says*:
    /// Returns the list appended with l
    public List<E> append(List<E> l) {
        List<E> t = new List<>(new ArrayList<>(this.data.size() + l.data.size()));
        t.data.addAll(this.data);
        t.data.addAll(l.data);
        return t;
    }

    /// *W3C says*:
    /// Returns the list appended with l
    public List<E> append_set(OrderedSet<E> l) {
        List<E> t = new List<>(new ArrayList<>(this.data.size() + l.data.size()));
        t.data.addAll(this.data);
        t.data.addAll(l.data);
        return t;
    }

    /// *W3C says*:
    /// Returns the list of elements that satisfy the predicate f
    /// # Actual Implementation:
    /// Can't name the function "filter" because this get in conflict with pre-defined "filter"
    /// that is introduced by the Iterator-implementation.
    public List<E> filter_by(Predicate<E> f) {
        List<E> t = new List<>(new ArrayList<>(data.size()));
        for (E e : this.data) {
            if (f.test(e)) {
                t.data.add(e);
            }
        }
        return t;
    }

    /// *W3C says*:
    /// Returns true if some element in the list satisfies the predicate f.  Returns false for an empty list.
    public boolean some(Predicate<E> f) {
        for (E e : this.data) {
            if (f.test(e)) {
                return true;
            }
        }
        return false;
    }

    /// *W3C says*:
    /// Returns true if every element in the list satisfies the predicate f.  Returns true for an empty list.
    public boolean every(Predicate<E> f) {
        for (E e : this.data) {
            if (!f.test(e)) {
                return false;
            }
        }
        return true;
    }

    /// Returns a sorted copy of the list.
    public List<E> sort(Comparator<E> compare) {
        List<E> t = new List<>(new ArrayList<>(data));
        t.data.sort(compare);
        return t;
    }

    /// Extension to support "for in" semantics.
    public Iterator<E> iterator() {
        return data.iterator();
    }

    /// Extension to support conversion to ordered sets.\
    /// Returns a new OrderedSet with copies of the elements in this list.
    /// Duplicates are removed.
    public OrderedSet<E> to_set() {
        OrderedSet<E> s = new OrderedSet<>(data.size());
        for (E e : data) {
            s.add(e);
        }
        return s;
    }

    /// Returns the last element as mutable reference.
    public E last() {
        return data.isEmpty() ? null : data.get(data.size() - 1);
    }
}
