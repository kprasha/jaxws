/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */

package com.sun.xml.ws.client.dispatch.rearch;

import com.sun.xml.ws.client.Stub;
import com.sun.xml.ws.sandbox.message.Message;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;

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


    // Todo: Needs change
    public DispatchImpl(QName port, Class<T> aClass, Service.Mode mode, Object obj) {
        super(null, null); //TODO: integrate Stub Class
    }

    // Todo: Needs change
    public DispatchImpl(QName port, JAXBContext jc, Service.Mode mode, Object obj) {
        super(null, null); //TODo: integrate Stub class
    }

    /**
     * @param msg
     * @return
     */
    protected abstract Message createMessage(T msg);
}