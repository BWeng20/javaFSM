package com.bw.fsm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Predicate;

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


    /// Extension: The size (only informational)
    public int size() {
        return data.size();
    }

    /// *W3C says*:
    /// Adds e to the set if it is not already a member
    public void add(E e) {
        if (!data.contains(e)) {
            data.add(e);
        }
    }

    /// *W3C says*:
    /// Deletes e from the set
    public void delete(E e) {
        data.remove(e);
    }

    /// *W3C says*:
    /// Adds all members of s that are not already members of the set
    /// (s must also be an OrderedSet)
    public void union(OrderedSet<E> s) {
        for (var si : s.data) {
            add(si);
        }
    }

    /// *W3C says*:
    /// Is e a member of set?
    public boolean isMember(E e) {
        return this.data.contains(e);
    }

    /// *W3C says*:
    /// Returns true if some element in the set satisfies the predicate f.
    ///
    /// Returns false for an empty set.
    public boolean some(Predicate<E> f) {
        for (var si : data) {
            if (f.test(si)) {
                return true;
            }
        }
        return false;
    }

    /// *W3C says*:
    /// Returns true if every element in the set satisfies the predicate f.
    ///
    /// Returns true for an empty set.
    public boolean every(Predicate<E> f) {
        for (var si : data) {
            if (!f.test(si)) {
                return false;
            }
        }
        return true;
    }

    /// *W3C says*:
    /// Returns true if this set and set s have at least one member in common
    public boolean hasIntersection(OrderedSet<E> s) {
        for (var si : data) {
            if (s.isMember(si)) {
                return true;
            }
        }
        return false;
    }

    /// *W3C says*:
    /// Is the set empty?
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /// *W3C says*:
    /// Remove all elements from the set (make it empty)
    public void clear() {
        data.clear();
    }

    /// *W3C says*:
    /// Converts the set to a list that reflects the order in which elements were originally added.
    ///
    /// In the case of sets created by intersection, the order of the first set (the one on which
    /// the method was called) is used
    ///
    /// In the case of sets created by union, the members of the first set (the one on which union
    /// was called) retain their original ordering while any members belonging to the second set only
    /// are placed after, retaining their ordering in their original set.
    public List<E> toList() {
        return new List(new ArrayList(data));
    }

    public OrderedSet<E> sort(Comparator<E> compare) {
        OrderedSet<E> t = new OrderedSet(new ArrayList(data));
        t.data.sort(compare);
        return t;
    }

    public Iterator<E> iterator() {
        return data.iterator();
    }
}
