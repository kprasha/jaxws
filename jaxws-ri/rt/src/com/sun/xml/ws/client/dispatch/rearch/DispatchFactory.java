/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client.dispatch.rearch;

import com.sun.xml.ws.client.dispatch.rearch.soapmsg.SOAPMessageDispatch;
import com.sun.xml.ws.client.dispatch.rearch.datasource.DataSourceDispatch;
import com.sun.xml.ws.client.dispatch.rearch.source.SourceDispatch;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.binding.BindingImpl;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.transform.Source;
import javax.activation.DataSource;

/**
* Class implementing this is DispatchFactoryImpl.
* Implemented class used to create a type-safe Dispatch<T> for
* Source.class, DataSource.class and SOAPMessage.class
* classes.
*/
public abstract class DispatchFactory {
    private DispatchFactory() {} // no instanciation please

    /**
     * Factory for SOAPMessage.class
     */
    public static Dispatch<SOAPMessage> createSAAJDispatch(QName port, Service.Mode mode, WSService owner, Pipe pipe, WSBinding binding) {
        return new SOAPMessageDispatch(port, SOAPMessage.class, mode, (WSServiceDelegate)owner, pipe, (BindingImpl)binding);
    }

    /**
     * Factory for DataSource.class
     */
    public static Dispatch<DataSource> createDataSourceDispatch(QName port, Service.Mode mode, WSService owner, Pipe pipe, WSBinding binding) {
        return new DataSourceDispatch(port, DataSource.class, mode, (WSServiceDelegate)owner, pipe, (BindingImpl)binding);
    }

    /**
     * Factory for Source.class
     */
    public static Dispatch<Source> createSourceDispatch(QName port, Service.Mode mode, WSService owner, Pipe pipe, WSBinding binding) {
        return new SourceDispatch(port, Source.class, mode, (WSServiceDelegate)owner, pipe, (BindingImpl)binding);
    }

    /**
     * Creator method for typed Dispatch<T>.
     *
     * @param port  QName for port specific Dispatch
     * @param clazz {@link Source}, {@link DataSource} or {@link SOAPMessage} class.
     * @param mode  Is this a {@link Dispatch} for a payload or a whole message.
     * @return Dispatch<T> Typed specific Dispatch
     */
    public static <T> Dispatch<T> createDispatch(QName port, Class<T> clazz, Service.Mode mode, WSService owner, Pipe pipe, WSBinding binding) {
        //todo://added just to get something going for dispatch

        if (clazz == SOAPMessage.class) {
            return (Dispatch<T>) createSAAJDispatch(port, mode, owner, pipe, binding);
        } else if (clazz == Source.class) {
            return (Dispatch<T>) createSourceDispatch(port, mode, owner, pipe, binding);
        } else if (clazz == DataSource.class) {
            return (Dispatch<T>) createDataSourceDispatch(port, mode, owner, pipe, binding);
        } else
            throw new WebServiceException("Unknown class type " + clazz.getName());
    }
}




