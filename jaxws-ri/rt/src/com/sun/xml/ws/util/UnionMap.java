package com.sun.xml.ws.util;

import java.util.Map;
import java.util.AbstractMap;
import java.util.Set;
import java.util.AbstractSet;
import java.util.Iterator;

/**
 * Creates a map view on top of two {@link Map}s.
 *
 * @author Kohsuke Kawaguchi
 */
public class UnionMap<K,V> extends AbstractMap<K,V> {

    /**
     * {@link Map}s whose union semantis we are creating.
     */
    private final Map<K,V> lhs,rhs;

    private final UnionSet unionSet = new UnionSet();

    public UnionMap(Map<K,V> lhs, Map<K,V> rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    private final class UnionSet extends AbstractSet<Entry<K,V>> {
        public Iterator<Entry<K,V>> iterator() {
            return new UnionIterator<Entry<K,V>>(lhs.entrySet().iterator(), rhs.entrySet().iterator());
        }

        public int size() {
            return lhs.size()+rhs.size();
        }
    }

    public Set<Entry<K, V>> entrySet() {
        return unionSet;
    }

    public V put(K key, V value) {
        if(lhs.containsKey(key))
            return lhs.put(key,value);
        else
            return rhs.put(key,value);
    }

    private static final class UnionIterator<V> implements Iterator<V> {
        private final Iterator<V> lhs;
        private final Iterator<V> rhs;

        private boolean lastWasLhs;

        public UnionIterator(Iterator<V> lhs, Iterator<V> rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        public boolean hasNext() {
            return lhs.hasNext() || rhs.hasNext();
        }

        public V next() {
            lastWasLhs = lhs.hasNext();
            if(lastWasLhs)  return lhs.next();
            else            return rhs.next();
        }

        public void remove() {
            if(lastWasLhs)  lhs.remove();
            else            rhs.remove();
        }
    }
}
