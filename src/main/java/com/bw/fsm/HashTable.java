package com.bw.fsm;

import java.util.HashMap;

/// *W3C says*:
/// table\[foo] returns the value associated with foo.
/// table\[foo] = bar sets the value associated with foo to be bar.
/// ### Actual implementation:
/// Instead of the Operators, methods are used.
public class HashTable<K, T> {
    public HashMap<K, T> data;

    public HashTable() {
        data = new HashMap();
    }

    /// Extension to re-use exiting instances.
    public void clear() {
        this.data.clear();
    }

    public void put(K k, T v) {
        this.data.put(k, v);
    }

    public void put_all(HashTable<K, T> t) {
        this.data.putAll(t.data);
    }

    public boolean has(K k) {
        return this.data.containsKey(k);
    }

    public T get(K k) {
        return this.data.get(k);
    }
}
