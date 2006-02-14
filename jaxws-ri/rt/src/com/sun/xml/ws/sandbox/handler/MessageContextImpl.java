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

import com.sun.xml.ws.util.PropertySet;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;

/**
 *
 * @author WS Development Team
 */
public class MessageContextImpl extends PropertySet implements MessageContext {
    
    /**
     * Value of {@link #MESSAGE_OUTBOUND_PROPERTY} property.
     */
    @Property(MessageContext.MESSAGE_OUTBOUND_PROPERTY)
    public boolean messageOutBound;
    
    /**
     * Value of {@link #INBOUND_MESSAGE_ATTACHMENTS} property
     */
    // TODO: do not compute these values eagerly.
    // allow ContextProperty to be on a method so that
    // this can be computed lazily
    @Property(MessageContext.INBOUND_MESSAGE_ATTACHMENTS)
    public Map<String, DataHandler> inboundMessageAttachments;

    /**
     * Value of {@link #OUTBOUND_MESSAGE_ATTACHMENTS} property
     */
    @Property(MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS)
    public Map<String, DataHandler> outboundMessageAttachments;
    
    
    /**
     * Value of {@link #WSDL_DESCRIPTION} property.
     */
    @Property(MessageContext.WSDL_DESCRIPTION)
    public org.xml.sax.InputSource wsdlDescription;
    
    /**
     * Value of {@link #WSDL_SERVICE} property.
     */
    @Property(MessageContext.WSDL_SERVICE)
    public QName wsdlService;

    /**
     * Value of {@link #WSDL_PORT} property.
     */
    @Property(MessageContext.WSDL_PORT)
    public QName wsdlPort;
    
    /**
     * Value of {@link #WSDL_INTERFACE} property.
     */
    @Property(MessageContext.WSDL_INTERFACE)
    public QName wsdlInterface;
    
    /**
     * Value of {@link #WSDL_OPERATION} property.
     */
    @Property(MessageContext.WSDL_OPERATION)
    public QName wsdlOperation;
    
    /**
     * Value of {@link #HTTP_RESPONSE_CODE} property.
     */
    @Property(MessageContext.HTTP_RESPONSE_CODE)
    public Integer httpResponseCode;
     
    /**
     * Value of {@link #HTTP_REQUEST_HEADERS} property.
     */
    @Property(MessageContext.HTTP_REQUEST_HEADERS)
    public Map<String, List<String>> httpRequestHeaders;
    
    /**
     * Value of {@link #HTTP_RESPONSE_HEADERS} property.
     */
    @Property(MessageContext.HTTP_RESPONSE_HEADERS)
    public Map<String, List<String>> httpResponseHeaders;

    /**
     * Value of {@link #HTTP_REQUEST_METHOD} property.
     */
    @Property(MessageContext.HTTP_REQUEST_METHOD)
    public String httpRequestMethod;
    
    /**
     * Value of {@link #SERVLET_REQUEST} property.
     */
    @Property(MessageContext.SERVLET_REQUEST)
    public Object servletRequest;
    
    /**
     * Value of {@link #SERVLET_RESPONSE} property.
     */
    @Property(MessageContext.SERVLET_RESPONSE)
    public Object servletResponse;
    
        
    /**
     * Value of {@link #SERVLET_CONTEXT} property.
     */
    @Property(MessageContext.SERVLET_CONTEXT)
    public Object servletContext;
    
    /** Creates a new instance of MessageContextImpl */
    public MessageContextImpl(Packet packet) {
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



    private static final PropertyMap model;

    static {
        model = parse(MessageContextImpl.class);
    }

    protected PropertyMap getPropertyMap() {
        return model;
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

    public Set<Entry<String, Object>> entrySet() {
        // TODO
        throw new UnsupportedOperationException();
    }
}
