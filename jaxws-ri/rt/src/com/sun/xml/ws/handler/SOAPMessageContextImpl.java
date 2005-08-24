/*
 * $Id: SOAPMessageContextImpl.java,v 1.7 2005-08-24 03:24:47 jitu Exp $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc.
 * All rights reserved.
 */
package com.sun.xml.ws.handler;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.security.SecurityConfiguration;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import com.sun.pept.ept.MessageInfo;
import com.sun.xml.ws.encoding.jaxb.JAXBBeanInfo;
import com.sun.xml.ws.encoding.jaxb.LogicalEPTFactory;
import com.sun.xml.ws.encoding.jaxb.LogicalEncoder;
import com.sun.xml.ws.encoding.soap.internal.InternalMessage;
import com.sun.xml.ws.spi.runtime.InternalSoapEncoder;
import java.lang.reflect.Method;

/**
 * Implementation of SOAPMessageContext. This class is used at runtime
 * to pass to the handlers for processing soap messages.
 *
 * @see MessageContextImpl
 *
 * @author WS Development Team
 */
public class SOAPMessageContextImpl implements SOAPMessageContext,
    com.sun.xml.ws.spi.runtime.SOAPMessageContext {

    private HandlerContext handlerCtxt;
    private MessageContext ctxt;
    private Set<String> roles;
    private static Map<String, Class> allowedTypes = null;
    private boolean failure;

    public SOAPMessageContextImpl(HandlerContext handlerCtxt) {
        this.handlerCtxt = handlerCtxt;
        this.ctxt = handlerCtxt.getMessageContext();
        if (allowedTypes == null) {
            allowedTypes = new HashMap<String, Class>();
            allowedTypes.put(MessageContext.MESSAGE_ATTACHMENTS, Map.class);
            allowedTypes.put(MessageContext.WSDL_DESCRIPTION, URI.class);
            allowedTypes.put(MessageContext.WSDL_SERVICE, QName.class);
            allowedTypes.put(MessageContext.WSDL_PORT, QName.class);
            allowedTypes.put(MessageContext.WSDL_INTERFACE, QName.class);
            allowedTypes.put(MessageContext.WSDL_OPERATION, QName.class);
            allowedTypes.put(MessageContext.MESSAGE_OUTBOUND_PROPERTY, Boolean.class);
            allowedTypes.put(MessageContext.MESSAGE_SECURITY_CONFIGURATION, SecurityConfiguration.class);
        }
    }

    public SOAPMessage getMessage() {
        SOAPMessage soap = handlerCtxt.getSOAPMessage();
        InternalMessage intr = handlerCtxt.getInternalMessage();
        if (intr == null && soap != null) {
            // Not much to do
        } else if (intr != null && soap != null) {
            // Overlay BodyBlock of InternalMessage on top of existing SOAPMessage
            MessageInfo messageInfo = handlerCtxt.getMessageInfo();
            LogicalEPTFactory eptf = (LogicalEPTFactory)messageInfo.getEPTFactory();
            soap = eptf.getSOAPEncoder().toSOAPMessage(intr, soap);
            setMessage(soap);        // It also sets InernalMessage to null
        } else if (intr != null && soap == null) {
            // Convert InternalMessage to a SOAPMessage
            MessageInfo messageInfo = handlerCtxt.getMessageInfo();
            LogicalEPTFactory eptf = (LogicalEPTFactory)messageInfo.getEPTFactory();
            soap = eptf.getSOAPEncoder().toSOAPMessage(intr, messageInfo);
            setMessage(soap);        // It also sets InernalMessage to null
        } else {
            throw new WebServiceException("Don't have SOAPMessage");
        }
        return soap;
    }

    public void setMessage(SOAPMessage soapMessage) {
        handlerCtxt.setSOAPMessage(soapMessage);
        // current InternalMessage is not valid anymore. So reset it.
        handlerCtxt.setInternalMessage(null);
    }

    /*
     * TODO: take care of allRoles parameter
     *
     */
    public Object[] getHeaders(QName header, JAXBContext jaxbContext, boolean allRoles) {
        try {
            List beanList = new ArrayList();
            SOAPMessage msg = getMessage();
            Name name = SOAPFactory.newInstance().createName(header.getLocalPart(),
                    header.getPrefix(), header.getNamespaceURI());
            SOAPHeader sHeader = msg.getSOAPHeader();
            if (sHeader == null) {
                return new Object[0];
            }
            Iterator i = sHeader.getChildElements(name);
            while(i.hasNext()) {
                SOAPHeaderElement child = (SOAPHeaderElement)i.next();
                Source source = new DOMSource(child);
                MessageInfo messageInfo = handlerCtxt.getMessageInfo();
                LogicalEPTFactory eptf = (LogicalEPTFactory)messageInfo.getEPTFactory();
                LogicalEncoder encoder = eptf.getLogicalEncoder();
                JAXBBeanInfo beanInfo = encoder.toJAXBBeanInfo(source, jaxbContext);
                beanList.add(beanInfo.getBean());
            }
            return beanList.toArray();
        } catch(Exception e) {
            throw new WebServiceException(e);
        }
    }

    // todo: change underlying set to URI instead of String?
    public Set<URI> getRoles() {
        try {
            Set<URI> uris = new HashSet<URI>(roles.size());
            for (String role : roles) {
                uris.add(new URI(role));
            }
            return uris;
        } catch (URISyntaxException e) {
            throw new WebServiceException(e);
        }
    }

    void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    private boolean validateProperty(String name, Object value) {
        if (allowedTypes.containsKey(name)) {
            Class clazz = allowedTypes.get(name);
            if (!(clazz.isInstance(value)))
                throw new HandlerException("handler.messageContext.invalid.class",
                        new Object[] { value, name });
        }

        return true;
    }

    public void setScope(String name, Scope scope) {
        ctxt.setScope(name, scope);
    }

    public Scope getScope(String name) {
        return ctxt.getScope(name);
    }

    /* java.util.Map methods below here */
    
    public void clear() {
        ctxt.clear();
    }

    public boolean containsKey(Object obj) {
        return ctxt.containsKey(obj);
    }

    public boolean containsValue(Object obj) {
        return ctxt.containsValue(obj);
    }

    public Set<Entry<String, Object>> entrySet() {
        return ctxt.entrySet();
    }

    public Object get(Object obj) {
        return ctxt.get(obj);
    }

    public boolean isEmpty() {
        return ctxt.isEmpty();
    }

    public Set<String> keySet() {
        return ctxt.keySet();
    }

    public Object put(String str, Object obj) {
        return ctxt.put(str, obj);
    }

    public void putAll(Map<? extends String, ? extends Object> map) {
        ctxt.putAll(map);
    }

    public Object remove(Object obj) {
        return ctxt.remove(obj);
    }

    public int size() {
        return ctxt.size();
    }

    public Collection<Object> values() {
        return ctxt.values();
    }
    
    /*
     * Returns the invocation method
     */
    public Method getMethod() {
        // TODO
        return null;
    }
    
    /**
     * If there is a SOAPMessage already, use getSOAPMessage(). Ignore all other
     * methods
     */
    public boolean isAlreadySoap() {
        return false;
    }
    
    /*
     * Returns InternalMessage's BodyBlock value
     */
    public Object getBody() {
        return null;
    }
    
    /*
     * Returns InternalMessage's HeaderBlock values
     */
    public List getHeaders() {
        return null;
    }
    
    /*
     * Use this MessageInfo to pass to InternalSoapEncoder write methods
     */
    public MessageInfo getMessageInfo() {
        return null;
    }
    
    /*
     * Encoder to marshall all JAXWS objects: RpcLitPayload, JAXBBridgeInfo etc
     */
    public InternalSoapEncoder getEncoder() {
        return null;
    }

}
    