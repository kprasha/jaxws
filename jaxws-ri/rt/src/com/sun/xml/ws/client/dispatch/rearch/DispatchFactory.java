/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client.dispatch.rearch;

import com.sun.xml.ws.client.dispatch.rearch.soapmsg.SOAPMessageDispatch;
import com.sun.xml.ws.client.dispatch.rearch.datasource.DataSourceDispatch;
import com.sun.xml.ws.client.dispatch.rearch.source.SourceDispatch;
import com.sun.xml.ws.sandbox.pipe.Pipe;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.Binding;
import javax.xml.transform.Source;
import javax.activation.DataSource;

/**
* Class implementing this is DispatchFactoryImpl.
* Implemented class used to create a type-safe Dispatch<T> for
* Source.class, DataSource.class and SOAPMessage.class
* classes.
*/
public interface DispatchFactory<T> {

    /**
     * Dispatch<T> creator method definition
     */
    public Dispatch createDispatch(QName port, Class<T> clazz, Service.Mode mode, Object delegate, Pipe pipe, javax.xml.ws.Binding binding);

   /**
   * Type specific factories with inner Dispatch<T> creator methods
   * Note: JAXB typed DispatchFactory not needed as we know by virtue of JAXBContext parameter
   * that it is a JAXBDispatch**
   */

   /** Factory for SOAPMessage.class */
   public static final DispatchFactory<SOAPMessage> SOAP_DISPATCH_FACTORY = new DispatchFactory<SOAPMessage>() {

        public Dispatch createDispatch(QName port, Class<SOAPMessage> clazz, Service.Mode mode, Object obj, Pipe pipe, javax.xml.ws.Binding binding) {
            return (Dispatch) new SOAPMessageDispatch(port, clazz, mode, obj, pipe, binding);
        }
    };

    /* Factory for DataSource.class */
    public static final DispatchFactory<DataSource> DATASOURCE_DISPATCH_FACTORY = new DispatchFactory<DataSource>() {

        public Dispatch createDispatch(QName port, Class<DataSource> clazz, Service.Mode mode, Object obj, Pipe pipe, javax.xml.ws.Binding binding) {
            return (Dispatch) new DataSourceDispatch(port, clazz, mode, obj, pipe, binding);
        }
    };

    /** Factory for Source.class */
    public static final DispatchFactory<Source> SOURCE_DISPATCH_FACTORY = new DispatchFactory<Source>() {

        public Dispatch createDispatch(QName port, Class<Source> clazz, Service.Mode mode, Object obj, Pipe pipe, javax.xml.ws.Binding binding) {
            return (Dispatch) new SourceDispatch(port, clazz, mode, obj, pipe, binding);
        }
    };    
}




