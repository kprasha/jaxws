/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */

package com.sun.xml.ws.client.dispatch.rearch.datasource;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.client.dispatch.rearch.DispatchImpl;

import javax.activation.DataSource;
import javax.xml.namespace.QName;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import java.util.concurrent.Future;

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
public class DataSourceDispatch extends DispatchImpl<DataSource> {

    public DataSourceDispatch(QName port, Class<DataSource> clazz, Service.Mode mode, WSServiceDelegate service, Pipe pipe, BindingImpl binding) {
       super(port, clazz, mode, service, pipe, binding);
    }

    protected Message createMessage(DataSource arg) {
        // TODO
        throw new UnsupportedOperationException();
    }

    protected DataSource toReturnValue(Message response) {
        // TODO
        throw new UnsupportedOperationException();
    }
}