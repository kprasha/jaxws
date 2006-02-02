/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */

package com.sun.xml.ws.client.dispatch;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.WSServiceDelegate;


import javax.activation.DataSource;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

/**
 * TODO: Use sandbox classes, update javadoc
 */

/**
 *
 * @author WS Development Team
 * @version 1.0
 */
public class DataSourceDispatch extends DispatchImpl<DataSource> {

    public DataSourceDispatch(QName port, Class<DataSource> clazz, Service.Mode mode, WSServiceDelegate service, Pipe pipe, BindingImpl binding) {
       super(port, clazz, mode, service, pipe, binding);
    }

    Packet createPacket(DataSource arg) {
        // TODO
        throw new UnsupportedOperationException();
    }

    DataSource toReturnValue(Packet response) {
        // TODO
        throw new UnsupportedOperationException();
    }
}