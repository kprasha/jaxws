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



    private static final Map<String,Property> model;

    static {
        model = parse(MessageContextImpl.class);
    }

    protected Map<String, Property> getPropertyMap() {
        return model;
    }
}
