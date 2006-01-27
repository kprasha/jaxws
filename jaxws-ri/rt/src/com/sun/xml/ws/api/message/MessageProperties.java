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

import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.client.BindingProviderProperties;
import com.sun.xml.ws.client.RequestContext;

import javax.activation.DataHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
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
 *  <li>Scope. Can someone sit down with me (Kohsuke) and tell me
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
    // TODO: do not compute these values eagerly.
    // allow ContextProperty to be on a method so that
    // this can be computed lazily
    @ContextProperty(INBOUND_MESSAGE_ATTACHMENTS)
    public Map<String, DataHandler> inboundMessageAttachments;

    /**
     * Value of {@link #OUTBOUND_MESSAGE_ATTACHMENTS} property
     */
    @ContextProperty(OUTBOUND_MESSAGE_ATTACHMENTS)
    public Map<String, DataHandler> outboundMessageAttachments;

    /**
     * True if this message came from a transport (IOW inbound),
     * and in paricular from a "secure" transport. A transport
     * needs to set this flag appropriately.
     *
     * <p>
     * This is a requirement from the security team.
     */
    // TODO: expose this as a property
    public boolean wasTransportSecure;

    /**
     * If a message originates from a proxy stub that implements
     * a port interface, this field is set to point to that object.
     *
     * TODO: who's using this property? 
     */
    @ContextProperty(BindingProviderProperties.JAXWS_CLIENT_HANDLE_PROPERTY)
    public Object proxy;

    /**
     * The endpoint address to which this message is sent to.
     *
     * <p>
     * The JAX-WS spec allows this to be changed for each message,
     * so it's designed to be a property.
     *
     * <p>
     * TODO: isn't this a part of {@link #requestContext}?
     * Or more generally, what's the relationship between
     * {@link MessageProperties} and {@link #requestContext}?
     */
    @ContextProperty(BindingProvider.ENDPOINT_ADDRESS_PROPERTY)
    public String endpointAddress;

    /**
     * The client appliation configures this map through
     * {@link BindingProvider#getRequestContext()}.
     */
    @ContextProperty(BindingProviderProperties.JAXWS_CONTEXT_PROPERTY)
    public RequestContext requestContext;

    /**
     * The value of the SOAPAction header associated with the message.
     *
     * <p>
     * For outgoing messages, the transport may sends out this value.
     * If this field is null, the transport may choose to send <tt>""</tt>
     * (quoted empty string.)
     *
     * For incoming messages, the transport will set this field.
     * If the incoming message did not contain the SOAPAction header,
     * the transport sets this field to null.
     *
     * <p>
     * If the value is non-null, it must be always in the quoted form.
     * The value can be null.
     *
     * <p>
     * Note that the way the transport sends this value out depends on
     * transport and SOAP version.
     *
     * For HTTP transport and SOAP 1.1, BP requires that SOAPAction
     * header is present (See {@BP R2744} and {@BP R2745}.) For SOAP 1.2,
     * this is moved to the parameter of the "application/soap+xml".
     */
    @ContextProperty(BindingProviderProperties.SOAP_ACTION_PROPERTY)
    public String soapAction;

    /**
     * Indicates whether the current message is a request of
     * an one-way operation.
     *
     * <p>
     * This property is used on the client-side for
     * outbound messages, so that the producer of a {@link Message}
     * can communicate to the intermediate (and terminal) {@link Pipe}s
     * about its knowledge.
     *
     * <p>
     * When this property is {@link Boolean#TRUE}, it means that the producer of
     * the {@link Message} definitely knows that it's a request
     * {@link Message} is for an one-way operation.
     *
     * <p>
     * When this property is {@link Boolean#FALSE}, it means that the producer of
     * the {@link Message} definitely knows that it's expecting
     * a response for this message.
     *
     * <p>
     * When this property is null, it means that the producer
     * of the {@link Message} does not know if a reply is expected
     * or not.
     * (To give you some idea about when this can happen,
     * sometimes we don't have any WSDL and so we can't tell.)
     *
     * <p>
     * No other {@link Boolean} instances are allowed.
     */
    @ContextProperty(BindingProviderProperties.ONE_WAY_OPERATION)
    public Boolean isOneWay;

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
     * @see ContextProperty
     */
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
