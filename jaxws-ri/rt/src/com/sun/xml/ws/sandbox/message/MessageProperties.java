package com.sun.xml.ws.sandbox.message;

import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.activation.DataHandler;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Properties associated with a {@link Message}.
 *
 * <p>
 * This implements {@link MessageContext}, as we want this object to
 * be returned where user applications expect {@link MessageContext}.
 *
 * <p>
 * {@link LogicalMessageContext} and {@link SOAPMessageContext} will
 * be implemented as a delegate to this object, since those interfaces
 * may replace the {@link Message} object.
 *
 * <p>
 * If there are properties known the JAX-WS statically, they should be
 * present on this class as fields with {@link ContextProperty} annotation.
 *
 * <h3>Implementation Note</h3>
 * <p>
 * This implementation is designed to favor access through fields, although
 * it still allows access through {@link Map} methods. This is based on
 * the assumption that most of time no user code really cares about
 * properties in {@link MessageContext}, and even those who does will
 * just use a few {@link #get(Object)} method at most.
 *
 *
 * <h3>TODO</h3>
 * <ol>
 *  <li>this class needs to be cloneable since Message is copiable.
 *  <li>The three live views aren't implemented correctly. It will be
 *      more work to do so, although I'm sure it's possible.
 *  <li>Scope. Can someone shit down with me (Kohsuke) and tell me
 *      how they work?
 * </ol>
 *
 * @author Kohsuke Kawaguchi
 */
@SuppressWarnings({"SuspiciousMethodCalls"})
public class MessageProperties implements MessageContext {
    /**
     * Value of {@link #HTTP_REQUEST_HEADERS} property.
     */
    @ContextProperty(HTTP_REQUEST_HEADERS)
    public Map<String, List<String>> httpRequestHeaders;
    
    /**
     * Value of {@link #HTTP_RESPONSE_HEADERS} property.
     */
    @ContextProperty(HTTP_RESPONSE_HEADERS)
    public Map<String, List<String>> httpResponseHeaders;

    /**
     * Value of {@link #INBOUND_MESSAGE_ATTACHMENTS} property
     */
    @ContextProperty(INBOUND_MESSAGE_ATTACHMENTS)
    public Map<String, DataHandler> inboundMessageAttachments;

    /**
     * Value of {@link #OUTBOUND_MESSAGE_ATTACHMENTS} property
     */
    @ContextProperty(OUTBOUND_MESSAGE_ATTACHMENTS)
    public Map<String, DataHandler> outboundMessageAttachments;


    /**
     * Bag to capture "other" properties that do not have
     * strongly-typed presence on this object.
     *
     * TODO: allocate this instance lazily.
     */
    private Map<String,Object> otherProperties = new HashMap<String, Object>();


    public int size() {
        int sz = otherProperties.size();
        for (StaticProperty sp : props.values()) {
            if(sp.hasValue(this))
                sz++;
        }
        return sz;
    }

    public boolean isEmpty() {
        int sz = otherProperties.size();
        if(sz>0)    return false;

        for (StaticProperty sp : props.values()) {
            if(sp.hasValue(this))
                return false;
        }
        return true;
    }

    public boolean containsKey(Object key) {
        return get(key)!=null;
    }

    public boolean containsValue(Object value) {
        return values().contains(value);
    }

    public Object get(Object key) {
        StaticProperty sp = props.get(key);
        if(sp!=null)
            return sp.get(this);

        return otherProperties.get(key);
    }

    public Object put(String key, Object value) {
        StaticProperty sp = props.get(key);
        if(sp!=null) {
            Object old = sp.get(this);
            sp.set(this,value);
            return old;
        } else {
            return otherProperties.put(key,value);
        }
    }

    public Object remove(Object key) {
        StaticProperty sp = props.get(key);
        if(sp!=null) {
            Object old = sp.get(this);
            sp.set(this,null);
            return old;
        } else {
            return otherProperties.remove(key);
        }
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        for (Entry<? extends String, ? extends Object> e : t.entrySet())
            put(e.getKey(),e.getValue());
    }

    public void clear() {
        // TODO: is this even allowed?
        otherProperties.clear();
        for (StaticProperty sp : props.values())
            sp.set(this,null);
    }

    public Set<String> keySet() {
        // TODO: implement it correctly. this needs to be a live view
        Set<String> keys = new HashSet<String>();
        keys.addAll(otherProperties.keySet());
        for (StaticProperty sp : props.values()) {
            if(sp.hasValue(this))
                keys.add(sp.getName());
        }
        return keys;
    }

    public Collection<Object> values() {
        // TODO: implement it correctly. this needs to be a live view
        Set<Object> values = new HashSet<Object>();
        values.addAll(otherProperties.values());

        for (StaticProperty sp : props.values()) {
            if(sp.hasValue(this))
                values.add(sp.get(this));
        }
        return values;
    }

    public Set<Entry<String,Object>> entrySet() {
        // TODO: implement it correctly. this needs to be a live view
        Set<Entry<String,Object>> values = new HashSet<Entry<String,Object>>();

        values.addAll(otherProperties.entrySet());

        for (final StaticProperty sp : props.values()) {
            if(sp.hasValue(this))
                values.add(new Entry<String,Object>() {
                    public String getKey() {
                        return sp.getName();
                    }

                    public Object getValue() {
                        return sp.get(MessageProperties.this);
                    }

                    public Object setValue(Object value) {
                        Object old = sp.get(MessageProperties.this);
                        sp.set(MessageProperties.this,value);
                        return old;
                    }
                });
        }
        return values;
    }

    public void setScope(String endpointURL, Scope scope) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public Scope getScope(String endpointURL) {
        // TODO
        throw new UnsupportedOperationException();
    }


    /**
     * Model of {@link MessageProperties} class.
     */
    private static final Map<String,StaticProperty> props;

    static {
        props = new HashMap<String,StaticProperty>();
        for (Field f : MessageProperties.class.getFields()) {
            ContextProperty cp = f.getAnnotation(ContextProperty.class);
            if(cp!=null)
                props.put(cp.value(), new StaticProperty(f, cp));
        }
    }


    /**
     * Represents a field that has {@link ContextProperty} annotation.
     */
    static final class StaticProperty {
        /**
         * Field with the annotation.
         */
        private final Field f;

        /**
         * {@link ContextProperty} annotation on {@link #f}.
         */
        final ContextProperty annotation;

        public StaticProperty(Field f, ContextProperty annotation) {
            this.f = f;
            this.annotation = annotation;
        }

        String getName() {
            return annotation.value();
        }
        boolean hasValue(MessageProperties props) {
            return get(props)!=null;
        }
        Object get(MessageProperties props) {
            try {
                return f.get(props);
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            }
        }
        void set(MessageProperties props, Object value) {
            try {
                f.set(props,value);
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            }
        }
    }
}
