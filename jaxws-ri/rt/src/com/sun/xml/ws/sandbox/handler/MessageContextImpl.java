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

package com.sun.xml.ws.sandbox.handler;

import com.sun.xml.ws.api.message.TypedMap;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.MessageProperties;
import com.sun.xml.ws.api.message.ContextProperty;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;

/**
 *
 * @author WS Development Team
 */
public class MessageContextImpl extends TypedMap implements MessageContext {
    
    /**
     * Value of {@link #MESSAGE_OUTBOUND_PROPERTY} property.
     */
    @ContextProperty(MessageContext.MESSAGE_OUTBOUND_PROPERTY)
    public boolean messageOutBound;
    
    /**
     * Value of {@link #INBOUND_MESSAGE_ATTACHMENTS} property
     */
    // TODO: do not compute these values eagerly.
    // allow ContextProperty to be on a method so that
    // this can be computed lazily
    @ContextProperty(MessageContext.INBOUND_MESSAGE_ATTACHMENTS)
    public Map<String, DataHandler> inboundMessageAttachments;

    /**
     * Value of {@link #OUTBOUND_MESSAGE_ATTACHMENTS} property
     */
    @ContextProperty(MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS)
    public Map<String, DataHandler> outboundMessageAttachments;
    
    
    /**
     * Value of {@link #WSDL_DESCRIPTION} property.
     */
    @ContextProperty(MessageContext.WSDL_DESCRIPTION)
    public org.xml.sax.InputSource wsdlDescription;
    
    /**
     * Value of {@link #WSDL_SERVICE} property.
     */
    @ContextProperty(MessageContext.WSDL_SERVICE)
    public QName wsdlService;

    /**
     * Value of {@link #WSDL_PORT} property.
     */
    @ContextProperty(MessageContext.WSDL_PORT)
    public QName wsdlPort;
    
    /**
     * Value of {@link #WSDL_INTERFACE} property.
     */
    @ContextProperty(MessageContext.WSDL_INTERFACE)
    public QName wsdlInterface;
    
    /**
     * Value of {@link #WSDL_OPERATION} property.
     */
    @ContextProperty(MessageContext.WSDL_OPERATION)
    public QName wsdlOperation;
    
    /**
     * Value of {@link #HTTP_RESPONSE_CODE} property.
     */
    @ContextProperty(MessageContext.HTTP_RESPONSE_CODE)
    public Integer httpResponseCode;
     
    /**
     * Value of {@link #HTTP_REQUEST_HEADERS} property.
     */
    @ContextProperty(MessageContext.HTTP_REQUEST_HEADERS)
    public Map<String, List<String>> httpRequestHeaders;
    
    /**
     * Value of {@link #HTTP_RESPONSE_HEADERS} property.
     */
    @ContextProperty(MessageContext.HTTP_RESPONSE_HEADERS)
    public Map<String, List<String>> httpResponseHeaders;

    /**
     * Value of {@link #HTTP_REQUEST_METHOD} property.
     */
    @ContextProperty(MessageContext.HTTP_REQUEST_METHOD)
    public String httpRequestMethod;
    
    /**
     * Value of {@link #SERVLET_REQUEST} property.
     */
    @ContextProperty(MessageContext.SERVLET_REQUEST)
    public javax.servlet.http.HttpServletRequest servletRequest;
    
    /**
     * Value of {@link #SERVLET_RESPONSE} property.
     */
    @ContextProperty(MessageContext.SERVLET_RESPONSE)
    public javax.servlet.http.HttpServletResponse servletResponse;
    
        
    /**
     * Value of {@link #SERVLET_CONTEXT} property.
     */
    @ContextProperty(MessageContext.SERVLET_CONTEXT)
    public javax.servlet.ServletContext servletContext;
    
    /** Creates a new instance of MessageContextImpl */
    public MessageContextImpl(Message msg) {
       //msgProps = msg.getProperties();
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
     * Bag to capture "other" properties that do not have
     * strongly-typed presence on this object.
     *
     * TODO: allocate this instance lazily.
     */
    private Map<String,Object> otherProperties = new HashMap<String, Object>();
    
    static {
        parse(MessageContextImpl.class);
    }
    
    public int size() {
        int sz = otherProperties.size();
        for (Property sp : props.values()) {
            if(sp.hasValue(this))
                sz++;
        }
        return sz;
    }

    public boolean isEmpty() {
        int sz = otherProperties.size();
        if(sz>0)    return false;
        for (Property sp : props.values()) {
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
        Property sp = props.get(key);
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
        Property sp = props.get(key);
        if(sp!=null) {
            Object old = sp.get(this);
            sp.set(this,value);
            return old;
        } else {
            return otherProperties.put(key,value);
        }
    }

    public Object remove(Object key) {
        Property sp = props.get(key);
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
        for (Property sp : props.values())
            sp.set(this,null);
    }

    public Set<String> keySet() {
        // TODO: implement it correctly. this needs to be a live view
        Set<String> keys = new HashSet<String>();
        keys.addAll(otherProperties.keySet());
        for (Property sp : props.values()) {
            if(sp.hasValue(this))
                keys.add(sp.getName());
        }
        return keys;
    }

    public Collection<Object> values() {
        // TODO: implement it correctly. this needs to be a live view
        Set<Object> values = new HashSet<Object>();
        values.addAll(otherProperties.values());

        for (Property sp : props.values()) {
            if(sp.hasValue(this))
                values.add(sp.get(this));
        }
        return values;
    }

    public Set<Entry<String,Object>> entrySet() {
        // TODO: implement it correctly. this needs to be a live view
        Set<Entry<String,Object>> values = new HashSet<Entry<String,Object>>();

        values.addAll(otherProperties.entrySet());

        for (final Property sp : props.values()) {
            if(sp.hasValue(this))
                values.add(new Entry<String,Object>() {
                    public String getKey() {
                        return sp.getName();
                    }

                    public Object getValue() {
                        return sp.get(MessageContextImpl.this);
                    }

                    public Object setValue(Object value) {
                        Object old = sp.get(MessageContextImpl.this);
                        sp.set(MessageContextImpl.this,value);
                        return old;
                    }
                });
        }
        return values;
    }
    

}
