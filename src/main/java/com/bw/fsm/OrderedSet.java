package com.bw.fsm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * Wrapper around a java.util.List, simulate the "OrderedSet" of the W3C Algorithm -
 * including some extensions needed.
 */
public class OrderedSet<E> {

    public final java.util.List<E> data;

    public OrderedSet() {
        this(new ArrayList<>());
    }

    public OrderedSet(int capacity) {
        this(new ArrayList<>(capacity));
    }

    public OrderedSet(java.util.List<E> data) {
        this.data = data;
    }

    /**
     * Extension: The size (only informational)
     */
    public int size() {
        return data.size();
    }

    /**
     * <b>W3C says</b>:<br>
     * Adds e to the set if it is not already a member
     */
    public void add(E e) {
        if (!data.contains(e)) {
            data.add(e);
        }
    }

    /**
     * <b>W3C says</b>:<br>
     * Deletes e from the set
     */
    public void delete(E e) {
        data.remove(e);
    }

    /**
     * <b>W3C says</b>:<br>
     * Adds all members of s that are not already members of the set
     * (s must also be an OrderedSet)
     */
    public void union(OrderedSet<E> s) {
        for (var si : s.data) {
            add(si);
        }
    }

    /**
     * <b>W3C says</b>:<br>
     * Is e a member of set?
     */
    public boolean isMember(E e) {
        return this.data.contains(e);
    }

    /**
     * <b>W3C says</b>:<br>
     * Returns true if some element in the set satisfies the predicate f.
     * <p>
     * Returns false for an empty set.
     */
    public boolean some(Predicate<E> f) {
        for (var si : data) {
            if (f.test(si)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <b>W3C says</b>:<br>
     * Returns true if every element in the set satisfies the predicate f.
     * <p>
     * Returns true for an empty set.
     */
    public boolean every(Predicate<E> f) {
        for (var si : data) {
            if (!f.test(si)) {
                return false;
            }
        }
        return true;
    }

    /**
     * <b>W3C says</b>:<br>
     * Returns true if this set and set s have at least one member in common
     */
    public boolean hasIntersection(OrderedSet<E> s) {
        for (var si : data) {
            if (s.isMember(si)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <b>W3C says</b>:<br>
     * Is the set empty?
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * <b>W3C says</b>:<br>
     * Remove all elements from the set (make it empty)
     */
    public void clear() {
        data.clear();
    }


    /**
     * <b>W3C says</b>:<br>
     * Converts the set to a list that reflects the order in which elements were originally added.
     * <p>
     * In the case of sets created by intersection, the order of the first set (the one on which
     * the method was called) is used
     * <p>
     * In the case of sets created by union, the members of the first set (the one on which union
     * was called) retain their original ordering while any members belonging to the second set only
     * are placed after, retaining their ordering in their original set.
     */
    public List<E> toList() {
        return new List<>(new ArrayList<>(data));
    }

    public OrderedSet<E> sort(Comparator<E> compare) {
        OrderedSet<E> t = new OrderedSet<>(new ArrayList<>(data));
        t.data.sort(compare);
        return t;
    }

    public Iterator<E> iterator() {
        return data.iterator();
    }
}
