/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.util;

import com.sun.xml.ws.api.message.Packet;

import javax.xml.ws.handler.MessageContext;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A set of "properties" that can be accessed via strongly-typed fields
 * as well as reflexibly through the property name.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class PropertySet {

    /**
     * Creates a new instance of TypedMap.
     */
    protected PropertySet() {}

    /**
     * Marks a field on {@link PropertySet} as a
     * property of {@link MessageContext}.
     *
     * <p>
     * To make the runtime processing easy, this annotation
     * must be on a public field (since the property value
     * can be set through {@link Map} anyway, you won't be
     * losing abstraction by doing so.)
     *
     * <p>
     * For similar reason, this annotation can be only placed
     * on a reference type, not primitive type.
     *
     * @see Packet
     * @author Kohsuke Kawaguchi
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD,ElementType.METHOD})
    public @interface Property {
        /**
         * Name of the property.
         */
        String value();
    }

    /**
     * Represents the list of strongly-typed known propertyies
     * (keyed by property names.)
     *
     * <p>
     * Just giving it an alias to make the use of this class more fool-proof.
     */
    protected static final class PropertyMap extends HashMap<String,Accessor> {}

    /**
     * Map representing the Fields and Methods annotated with {@link Property}.
     * Model of {@link PropertySet} class.
     */
    protected abstract PropertyMap getPropertyMap();

    /**
     * This method parses a class for fields and methods with {@link Property}.
     */
    protected static PropertyMap parse(Class clazz) {
        PropertyMap props = new PropertyMap();
        for (Field f : clazz.getFields()) {
            Property cp = f.getAnnotation(Property.class);
            if(cp!=null)
                props.put(cp.value(), new FieldAccessor(f, cp));
        }
        for (Method m : clazz.getMethods()) {
            Property cp = m.getAnnotation(Property.class);
            if(cp!=null) {
                try {
                    String name = m.getName();
                    assert name.startsWith("get");  // must be on the getter
                    name = 's'+name.substring(1);   // getFoo -> setFoo
                    Method setter = clazz.getMethod(name,m.getReturnType());
                    props.put(cp.value(), new MethodAccessor(m,setter,cp));
                } catch (NoSuchMethodException e) {
                    throw new Error(e);     // that's a bug. define the setter please.
                }
            }
        }

        return props;
    }

    /**
     * Represents a typed property defined on a {@link PropertySet}.
     */
    protected interface Accessor {
        String getName();
        boolean hasValue(PropertySet props);
        Object get(PropertySet props);
        void set(PropertySet props, Object value);
    }

    static final class FieldAccessor implements Accessor {
        /**
         * Field with the annotation.
         */
        private final Field f;

        /**
         * {@link Property} annotation on {@link #f}.
         */
        final Property annotation;

        protected FieldAccessor(Field f, Property annotation) {
            this.f = f;
            this.annotation = annotation;
        }

        public String getName() {
            return annotation.value();
        }

        public boolean hasValue(PropertySet props) {
            return get(props)!=null;
        }

        public Object get(PropertySet props) {
            try {
                return f.get(props);
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            }
        }

        public void set(PropertySet props, Object value) {
            try {
                f.set(props,value);
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            }
        }
    }

    static final class MethodAccessor implements Accessor {
        /**
         * Getter method.
         */
        private final Method getter;
        /**
         * Setter method.
         */
        private final Method setter;

        /**
         * {@link Property} annotation on {@link #getter}.
         */
        final Property annotation;

        protected MethodAccessor(Method getter, Method setter, Property annotation) {
            this.getter = getter;
            this.setter = setter;
            this.annotation = annotation;
        }

        public String getName() {
            return annotation.value();
        }

        public boolean hasValue(PropertySet props) {
            return get(props)!=null;
        }

        public Object get(PropertySet props) {
            try {
                return getter.invoke(props);
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            } catch (InvocationTargetException e) {
                handle(e);
                return 0;   // never reach here
            }
        }

        public void set(PropertySet props, Object value) {
            try {
                setter.invoke(props,value);
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            } catch (InvocationTargetException e) {
                handle(e);
            }
        }

        /**
         * Since we don't expect the getter/setter to throw a checked exception,
         * it should be possible to make the exception propagation transparent.
         * That's what we are trying to do here.
         */
        private Exception handle(InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if(t instanceof Error)
                throw (Error)t;
            if(t instanceof RuntimeException)
                throw (RuntimeException)t;
            throw new Error(e);
        }
    }


    public boolean containsKey(Object key) {
        return get(key)!=null;
    }

    public Object get(Object key) {
        Accessor sp = getPropertyMap().get(key);
        if(sp!=null)
            return sp.get(this);
        throw new IllegalArgumentException("Undefined property "+key);
    }

    /**
     * Sets a property.
     *
     * <h3>Implementation Note</h3>
     * This method is slow. Code inside JAX-WS should define strongly-typed
     * fields in this class and access them directly, instead of using this.
     *
     * @throws IllegalArgumentException
     *      if the given key is an alias of a strongly-typed field,
     *      and if the value object given is not assignable to the field.
     *
     * @see Property
     */
    public Object put(String key, Object value) {
        Accessor sp = getPropertyMap().get(key);
        if(sp!=null) {
            Object old = sp.get(this);
            sp.set(this,value);
            return old;
        } else {
            throw new IllegalArgumentException("Undefined property "+key);
        }
    }

    /**
     * Checks if this {@link PropertySet} supports a property of the given name.
     */
    public boolean supports(Object key) {
        return getPropertyMap().containsKey(key);
    }

    public Object remove(Object key) {
        Accessor sp = getPropertyMap().get(key);
        if(sp!=null) {
            Object old = sp.get(this);
            sp.set(this,null);
            return old;
        } else {
            throw new IllegalArgumentException("Undefined property "+key);
        }
    }

    /**
     * Creates a {@link Map} view of this {@link PropertySet}.
     *
     * <p>
     * This map is live, in the sense that values you set to it
     * will be reflected to {@link PropertySet}, and changes made
     * to {@link PropertySet} externally would still see through
     * the map view.
     *
     * @return
     *      always non-null valid instance.
     */
    public Map<String,Object> createMapView() {
        final Set<Map.Entry<String,Object>> core = new HashSet<Entry<String, Object>>();

        for (final Entry<String, Accessor> e : getPropertyMap().entrySet()) {
            core.add(new Entry<String, Object>() {
                public String getKey() {
                    return e.getKey();
                }

                public Object getValue() {
                    return e.getValue().get(PropertySet.this);
                }

                public Object setValue(Object value) {
                    Accessor acc = e.getValue();
                    Object old = acc.get(PropertySet.this);
                    acc.set(PropertySet.this,value);
                    return old;
                }
            });
        }

        return new AbstractMap<String, Object>() {
            public Set<Entry<String,Object>> entrySet() {
                return core;
            }
        };
    }
}
