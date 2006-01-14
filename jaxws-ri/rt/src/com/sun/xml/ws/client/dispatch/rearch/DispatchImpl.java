/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */

package com.sun.xml.ws.client.dispatch.rearch;

import com.sun.xml.ws.client.Stub;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.client.dispatch.DispatchContext;
import static com.sun.xml.ws.client.BindingProviderProperties.JAXWS_CONTEXT_PROPERTY;
import static com.sun.xml.ws.client.BindingProviderProperties.BINDING_ID_PROPERTY;
import static com.sun.xml.ws.client.BindingProviderProperties.JAXWS_CLIENT_HANDLE_PROPERTY;
import static com.sun.xml.ws.client.BindingProviderProperties.JAXB_CONTEXT_PROPERTY;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.message.MessageProperties;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.encoding.soap.SOAPVersion;
import com.sun.xml.ws.binding.BindingImpl;
//import com.sun.xml.ws.model.JavaMethod;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.*;

/**
 * TODO: Use sandbox classes, update javadoc
 */
/**
 * The <code>javax.xml.ws.Dispatch</code> interface provides support
 * for the dynamic invocation of a service endpoint operation using XML
 * constructs or JAXB objects. The <code>javax.xml.ws.Service</code>
 * interface acts as a factory for the creation of <code>Dispatch</code>
 * instances.
 *
 * @author WS Development Team
 * @version 1.0
 */

public abstract class DispatchImpl<T> extends Stub implements Dispatch<T> {

    protected Service.Mode mode;
    protected QName portname;
    protected Class<T> clazz;
    protected Object owner;
    protected JAXBContext jaxbcontext;
    protected SOAPVersion soapVersion;


    /**
     *
     * @param port
     * @param aClass
     * @param mode
     * @param obj
     * @param pipe
     * @param binding
     */
    public DispatchImpl(QName port, Class<T> aClass, Service.Mode mode, Object obj, Pipe pipe, javax.xml.ws.Binding binding) {
        this(port, mode, obj, pipe, binding);
        this.clazz = aClass;


    }

    /**
     *
     * @param port
     * @param jc
     * @param mode
     * @param obj
     * @param pipe
     * @param binding
     */
    public DispatchImpl(QName port, JAXBContext jc, Service.Mode mode, Object obj, Pipe pipe, javax.xml.ws.Binding binding) {
        this(port, mode, obj, pipe, binding);
        jaxbcontext = jc;

    }

    /**
     *
     * @param port
     * @param mode
     * @param obj
     * @param pipe
     * @param binding
     */
    public DispatchImpl(QName port, Service.Mode mode, Object obj, Pipe pipe, javax.xml.ws.Binding binding){
        super(pipe, binding);
        portname = port;
        this.mode = mode;
        owner = obj;
        this.soapVersion = SOAPVersion.fromBinding(((BindingImpl)binding).getBindingId());
    }


    /**
     * @param msg
     * @return
     */
    protected abstract Message createMessage(T msg);


    //todo: temp just to get something working
    protected void setProperties(Message msg) {

        MessageProperties props = msg.getProperties();
        props.put(JAXWS_CLIENT_HANDLE_PROPERTY, this);
        props.put(ENDPOINT_ADDRESS_PROPERTY, ((WSServiceDelegate) owner).getEndpointAddress(portname));

        props.put(BINDING_ID_PROPERTY, ((BindingImpl)binding).getBindingId());
        if (jaxbcontext != null)
            props.put(JAXB_CONTEXT_PROPERTY, jaxbcontext);


        //not needed but leave for now --maybe mode is needed
        props.put(DispatchContext.DISPATCH_MESSAGE_MODE, mode);
        if (clazz != null)
            props.put(DispatchContext.DISPATCH_MESSAGE_CLASS, clazz);
        props.put("SOAPVersion", soapVersion);
        props.put(JAXWS_CONTEXT_PROPERTY, getRequestContext());

        

    }

}