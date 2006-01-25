/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */

package com.sun.xml.ws.client.dispatch.rearch.soapmsg;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.client.BindingProviderProperties;
import com.sun.xml.ws.client.dispatch.rearch.DispatchImpl;
import com.sun.xml.ws.sandbox.message.impl.saaj.SAAJMessage;
import com.sun.xml.ws.util.Pool;

import javax.xml.namespace.QName;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
/**
 * TODO: Use sandbox classes, update javadoc
 */

/**
 * The <code>SOAPMessageDispatch</code> class provides support
 * for the dynamic invocation of a service endpoint operation using
 * the <code>SOAPMessage</code> class. The <code>javax.xml.ws.Service</code>
 * interface acts as a factory for the creation of <code>SOAPMessageDispatch</code>
 * instances.
 *
 * @author WS Development Team
 * @version 1.0
 */

public class SOAPMessageDispatch extends DispatchImpl<SOAPMessage> {
    Pool.Marshaller marshallers;


    /**
     * @param port
     * @param aClass
     * @param mode
     * @param owner
     */
    public SOAPMessageDispatch(QName port, Class<SOAPMessage> aClass, Service.Mode mode, WSServiceDelegate owner, Pipe pipe, BindingImpl binding) {
        super(port, aClass, mode, owner, pipe, binding);
    }

    /**
     * Invoke a service operation synchronously.
     * <p/>
     * The client is responsible for ensuring that the <code>msg</code> object
     * is formed according to the requirements of the protocol binding in use.
     *
     * @param msg A <code>SOAPMessage</code> object that will form the
     *            the message used to invoke the operation. Must be an instance
     *            of a <code>SOAPMessage</code>.
     * @return The response to the operation invocation. The object is
     *         is an instance of a <code>SOAPMessage</code>.
     * @throws javax.xml.ws.WebServiceException
     *          If there is any error in the configuration of
     *          the <code>SOAPMessageDispatch</code> instance
     */
    public SOAPMessage invoke(SOAPMessage msg)
        throws WebServiceException {
        Message message = createMessage(msg);
        setProperties(message);
        Message response = process(message);
        try {
            return response.readAsSOAPMessage();
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * @param arg
     * @return
     */
    protected Message createMessage(SOAPMessage arg) {
        MimeHeaders mhs = arg.getMimeHeaders();
        mhs.addHeader("Content-Type", "text/xml");
        mhs.addHeader("Content-Transfer-Encoding", "binary");
        Map<String, List<String>> ch = new HashMap<String, List<String>>();
        for (Iterator iter = arg.getMimeHeaders().getAllHeaders(); iter.hasNext();)
        {
            List<String> h = new ArrayList<String>();
            MimeHeader mh = (MimeHeader) iter.next();

            h.clear();
            h.add(mh.getValue());
            ch.put(mh.getName(), h);
        }

        Message msg = new SAAJMessage(arg);
        msg.getProperties().httpRequestHeaders = ch;
        return msg;
    }

    /**
     * Invoke a service operation asynchronously.  The
     * method returns without waiting for the response to the operation
     * invocation, the results of the operation are obtained by polling the
     * returned <code>Response</code>.
     * <p/>
     * The client is responsible for ensuring that the <code>msg</code> object
     * when marshalled is formed according to the requirements of the protocol
     * binding in use.
     *
     * @param msg An object that, when marshalled, will form
     *            the message used to invoke the operation. Must be an instance
     *            of a <code>SOAPMessage</code>.
     * @return The response to the operation invocation. The object
     *         returned by <code>Response.get()</code> is must be
     *         an instance of a <code>SOAPMessage</code>
     * @throws javax.xml.ws.WebServiceException
     *          If there is any error in the configuration of
     *          the <code>SOAPMessageDispatch</code> instance
     */
    public Response<SOAPMessage> invokeAsync(SOAPMessage msg)
        throws WebServiceException {
        throw new UnsupportedOperationException();
    }


    /**
     * Invoke a service operation asynchronously. The
     * method returns without waiting for the response to the operation
     * invocation, the results of the operation are communicated to the client
     * via the passed in handler.
     * <p/>
     * The client is responsible for ensuring that the <code>msg</code> object
     * when marshalled is formed according to the requirements of the protocol
     * binding in use.
     *
     * @param msg     An object that, when marshalled, will form the
     *                the message used to invoke the operation. Must be an instance of
     *                a <code>SOAPMessage</code>.
     * @param handler The handler object that will receive the
     *                response to the operation invocation. The object
     *                returned by <code>Response.get()</code> is
     *                an instance of <code>SOAPMessage</code>.
     * @return A <code>Future</code> object that may be used to check the status
     *         of the operation invocation. This object must not be used to try to
     *         obtain the results of the operation - the object returned from
     *         <code>Future<?>.get()</code> is implementation dependent
     *         and any use of it will result in non-portable behaviour.
     * @throws javax.xml.ws.WebServiceException
     *          If there is any error in the configuration of
     *          the <code>Dispatch</code> instance
     */
    public Future<?> invokeAsync(SOAPMessage msg, AsyncHandler<SOAPMessage> handler) {
        throw new UnsupportedOperationException();
    }

    /**
     * Invokes a service operation using the one-way
     * interaction mode. The operation invocation is logically non-blocking,
     * subject to the capabilities of the underlying protocol, no results
     * are returned. When
     * the protocol in use is SOAP/HTTP, this method must block until
     * an HTTP response code has been received or an error occurs.
     * <p/>
     * The client is responsible for ensuring that the <code>msg</code> object
     * when marshalled is formed according to the requirements of the protocol
     * binding in use.
     *
     * @param msg An object that, when marshalled, will form the
     *            the message used to invoke the operation. Must be an instance of
     *            <code>SOAPMessage</code>.
     * @throws javax.xml.ws.WebServiceException
     *          If there is any error in the configuration of
     *          the <code>SOAPMessageDispatch</code> instance or if an error occurs during the
     *          invocation.
     */

    public void invokeOneWay(SOAPMessage msg) {
        //todo:not complete
        Message message = createMessage(msg);
        setProperties(message,Boolean.TRUE);
        Message result = process(message);

    }

    protected void setProperties(Message msg,boolean oneway){
        super.setProperties(msg);
        if (oneway)
            msg.getProperties().put(BindingProviderProperties.ONE_WAY_OPERATION, Boolean.TRUE);
    }
}