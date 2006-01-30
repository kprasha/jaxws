/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */

package com.sun.xml.ws.api.message;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

/**
 *
 * @author WS Development Team
 */
public abstract class TypedMap implements Map<String,Object> {

    /** Creates a new instance of TypedMap */
    protected TypedMap() {
    }

    /**
     * Map representing the Fields and Methods annotated with @ContextProperty.
     * Model of {@link TypedMap} class.
     */
    protected abstract Map<String,Property> getPropertyMap();

    /**
     * This method parses a class for fields and methods with {@link ContextProperty}.
     */
    protected static Map<String,Property> parse(Class clazz) {
        Map<String,Property> props = new HashMap<String,Property>();
        for (Field f : clazz.getClass().getFields()) {
            ContextProperty cp = f.getAnnotation(ContextProperty.class);
            if(cp!=null)
                props.put(cp.value(), new FieldProperty(f, cp));
        }

        for (Method m : clazz.getClass().getMethods()) {
            ContextProperty cp = m.getAnnotation(ContextProperty.class);
            // if(cp!=null) props.put(cp.value(), new MethodProperty(m, cp));
        }

        return props;
    }

    /**
     * The interface <code>Property</code> represents an
     * entry in <code>TypedMap<code>.
     *
     * @author WS Development Team
     */

    protected interface Property {
        String getName();

        boolean hasValue(TypedMap props);

        Object get(TypedMap props);

        void set(TypedMap props, Object value);

    }

    static final class FieldProperty implements Property {
        /**
         * Field with the annotation.
         */
        private final Field f;

        /**
         * {@link ContextProperty} annotation on {@link #f}.
         */
        final ContextProperty annotation;

        protected FieldProperty(Field f, ContextProperty annotation) {
            this.f = f;
            this.annotation = annotation;
        }

        public String getName() {
            return annotation.value();
        }

        public boolean hasValue(TypedMap props) {
            return get(props)!=null;
        }

        public Object get(TypedMap props) {
            try {
                return f.get(props);
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            }
        }

        public void set(TypedMap props, Object value) {
            try {
                f.set(props,value);
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            }
        }
    }


    public int size() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public boolean isEmpty() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public boolean containsKey(Object key) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public boolean containsValue(Object value) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public Object get(Object key) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public Object put(String key, Object value) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public Object remove(Object key) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public void clear() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public Set<String> keySet() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public Collection<Object> values() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public Set<Entry<String,Object>> entrySet() {
        // TODO
        throw new UnsupportedOperationException();
    }
    // resurrect these code

    //public int size() {
    //    int sz = otherProperties.size();
    //    for (Property sp : props.values()) {
    //        if(sp.hasValue(this))
    //            sz++;
    //    }
    //    return sz;
    //}
    //
    //public boolean isEmpty() {
    //    int sz = otherProperties.size();
    //    if(sz>0)    return false;
    //    for (Property sp : props.values()) {
    //        if(sp.hasValue(this))
    //            return false;
    //    }
    //    return true;
    //}
    //
    //public boolean containsKey(Object key) {
    //    return get(key)!=null;
    //}
    //
    //public boolean containsValue(Object value) {
    //    return values().contains(value);
    //}
    //
    //public Object get(Object key) {
    //    Property sp = props.get(key);
    //    if(sp!=null)
    //        return sp.get(this);
    //    return otherProperties.get(key);
    //}
    //
    ///**
    // * Sets a property.
    // *
    // * <h3>Implementation Note</h3>
    // * This method is slow. Code inside JAX-WS should define strongly-typed
    // * fields in this class and access them directly, instead of using this.
    // *
    // * @throws IllegalArgumentException
    // *      if the given key is an alias of a strongly-typed field,
    // *      and if the value object given is not assignable to the field.
    // *
    // * @see ContextProperty
    // */
    //public Object put(String key, Object value) {
    //    Property sp = props.get(key);
    //    if(sp!=null) {
    //        Object old = sp.get(this);
    //        sp.set(this,value);
    //        return old;
    //    } else {
    //        return otherProperties.put(key,value);
    //    }
    //}
    //
    //public Object remove(Object key) {
    //    Property sp = props.get(key);
    //    if(sp!=null) {
    //        Object old = sp.get(this);
    //        sp.set(this,null);
    //        return old;
    //    } else {
    //        return otherProperties.remove(key);
    //    }
    //}
    //
    //public void putAll(Map<? extends String, ? extends Object> t) {
    //    for (Entry<? extends String, ? extends Object> e : t.entrySet())
    //        put(e.getKey(),e.getValue());
    //}
    //
    //public void clear() {
    //    // TODO: is this even allowed?
    //    otherProperties.clear();
    //    for (Property sp : props.values())
    //        sp.set(this,null);
    //}
    //
    //public Set<String> keySet() {
    //    // TODO: implement it correctly. this needs to be a live view
    //    Set<String> keys = new HashSet<String>();
    //    keys.addAll(otherProperties.keySet());
    //    for (Property sp : props.values()) {
    //        if(sp.hasValue(this))
    //            keys.add(sp.getName());
    //    }
    //    return keys;
    //}
    //
    //public Collection<Object> values() {
    //    // TODO: implement it correctly. this needs to be a live view
    //    Set<Object> values = new HashSet<Object>();
    //    values.addAll(otherProperties.values());
    //
    //    for (Property sp : props.values()) {
    //        if(sp.hasValue(this))
    //            values.add(sp.get(this));
    //    }
    //    return values;
    //}
    //
    //public Set<Entry<String,Object>> entrySet() {
    //    // TODO: implement it correctly. this needs to be a live view
    //    Set<Entry<String,Object>> values = new HashSet<Entry<String,Object>>();
    //
    //    values.addAll(otherProperties.entrySet());
    //
    //    for (final Property sp : getPropertyMap().values()) {
    //        if(sp.hasValue(this))
    //            values.add(new Entry<String,Object>() {
    //                public String getKey() {
    //                    return sp.getName();
    //                }
    //
    //                public Object getValue() {
    //                    return sp.get(TypedMap.this);
    //                }
    //
    //                public Object setValue(Object value) {
    //                    Object old = sp.get(TypedMap.this);
    //                    sp.set(TypedMap.this,value);
    //                    return old;
    //                }
    //            });
    //    }
    //    return values;
    //}


}
