/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */

package com.sun.xml.ws.client.dispatch.rearch;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.MessageProperties;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.binding.BindingImpl;
import static com.sun.xml.ws.client.BindingProviderProperties.*;
import com.sun.xml.ws.client.Stub;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.client.dispatch.DispatchContext;

import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;

/**
 * TODO: update javadoc, use sandbox classes where can
 */
/**
 * The <code>DispatchImpl</code> abstract class provides support
 * for the dynamic invocation of a service endpoint operation using XML
 * constructs, JAXB objects or <code>SOAPMessage</code>. The <code>javax.xml.ws.Service</code>
 * interface acts as a factory for the creation of <code>DispatchImpl</code>
 * instances.
 *
 * @author WS Development Team
 * @version 1.0
 */
public abstract class DispatchImpl<T> extends Stub implements Dispatch<T> {

    protected final Service.Mode mode;
    protected final QName portname;
    protected Class<T> clazz;
    protected final WSServiceDelegate owner;
    protected final SOAPVersion soapVersion;


    protected DispatchImpl(QName port, Class<T> aClass, Service.Mode mode, WSServiceDelegate owner, Pipe pipe, BindingImpl binding) {
        this(port, mode, owner, pipe, binding);
        this.clazz = aClass;
    }

    protected DispatchImpl(QName port, Service.Mode mode, WSServiceDelegate owner, Pipe pipe, BindingImpl binding){
        super(pipe, binding);
        this.portname = port;
        this.mode = mode;
        this.owner = owner;
        this.soapVersion = binding.getSOAPVersion();
    }


    protected abstract Message createMessage(T msg);


    //todo: temp just to get something working

    /**
     *
     * @param msg
     */
    protected void setProperties(Message msg) {

        MessageProperties props = msg.getProperties();
        props.put(JAXWS_CLIENT_HANDLE_PROPERTY, this);
        props.put(ENDPOINT_ADDRESS_PROPERTY, owner.getEndpointAddress(portname));

        props.put(BINDING_ID_PROPERTY, binding.getBindingId());

        //not needed but leave for now --maybe mode is needed
        props.put(DispatchContext.DISPATCH_MESSAGE_MODE, mode);
        if (clazz != null)
            props.put(DispatchContext.DISPATCH_MESSAGE_CLASS, clazz);
        props.put("SOAPVersion", soapVersion);
        props.put(JAXWS_CONTEXT_PROPERTY, getRequestContext());
    }

}