/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */

package com.sun.xml.ws.client.dispatch.rearch.source;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.client.BindingProviderProperties;
import static com.sun.xml.ws.client.BindingProviderProperties.JAXB_CONTEXT_PROPERTY;
import com.sun.xml.ws.client.dispatch.rearch.DispatchImpl;
import com.sun.xml.ws.sandbox.message.impl.saaj.SAAJMessage;
import com.sun.xml.ws.sandbox.message.impl.source.PayloadSourceMessage;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
/**
 * TODO: Use sandbox classes, update javadoc
 */

/**
 * The <code>SourceDispatch</code> class provides support
 * for the dynamic invocation of a service endpoint operation using XML
 * constructs. The <code>javax.xml.ws.Service</code>
 * interface acts as a factory for the creation of <code>SourceDispatch</code>
 * instances.
 *
 * @author WS Development Team
 * @version 1.0
 */

public class SourceDispatch extends DispatchImpl<Source> {

    public SourceDispatch(QName port, Class<Source> aClass, Service.Mode mode, WSServiceDelegate owner, Pipe pipe, BindingImpl binding) {
        super(port, aClass, mode, owner, pipe, binding);
    }


    /**
     * Invoke a service operation synchronously.
     * <p/>
     * The client is responsible for ensuring that the <code>Source</code> msg
     * is formed according to the requirements of the protocol binding in use.
     *
     * @param msg An object that will form the payload of
     *            the message used to invoke the operation. Must be an instance of
     *            either <code>javax.xml.transform.Source</code>.
     * @return The response to the operation invocation. The object is
     *         either an instance of <code>javax.xml.transform.Source</code>
     *         or a JAXB object.
     * @throws javax.xml.ws.WebServiceException
     *          If there is any error in the configuration of
     *          the <code>SourceDispatch</code> instance
     */
    public Source invoke(Source msg)
        throws WebServiceException {
        Message message = createMessage(msg);
        setProperties(message);
        Message response = process(message);
        switch (mode){
            case PAYLOAD:
                return response.readPayloadAsSource();
            case MESSAGE:
                return response.readEnvelopeAsSource();
            default:
                throw new WebServiceException("Unrecognized dispatch mode");
        }       
    }

    /**
     * Invoke a service operation asynchronously.  The
     * method returns without waiting for the response to the operation
     * invocation, the results of the operation are obtained by polling the
     * returned <code>Response</code>.
     * <p/>
     * The client is responsible for ensuring that the <code>msg</code>
     * <code>javax.xml.transform.Source<code> when marshalled is formed
     * according to the requirements of the protocol
     * binding in use.
     *
     * @param msg A <code>javax.xml.transform.Source</code> that, when
     *            marshalled, will form the payload of the message used to
     *            invoke the operation. Must be an instance of
     *            either <code>javax.xml.transform.Source</code>. If
     * @return The response to the operation invocation. The object
     *         returned by <code>Response.get()</code> is
     *         either an instance of <code>javax.xml.transform.Source</code>
     *         object.
     * @throws javax.xml.ws.WebServiceException
     *          If there is any error in the configuration of
     *          the <code>SourceDispatch</code> instance
     */
    public Response<Source> invokeAsync(Source msg)
        throws WebServiceException {
        return super.invokeAsync(msg);

    }


    /**
     * Invoke a service operation asynchronously. The
     * method returns without waiting for the response to the operation
     * invocation, the results of the operation are communicated to the client
     * via the passed in handler.
     * <p/>
     * The client is responsible for ensuring that the <code>Source</code> msg
     * when marshalled is formed according to the requirements of the protocol
     * binding in use.
     *
     * @param msg     An <code>Source</code> that, when marshalled, will form the payload of
     *                the message used to invoke the operation. Must be an instance of
     *                either <code>javax.xml.transform.Source</code>.
     * @param handler The handler object that will receive the
     *                response to the operation invocation. The object
     *                returned by <code>Response.get()</code> is
     *                either an instance of
     *                <code>javax.xml.transform.Source</code>.
     * @return A <code>Future</code> object that may be used to check the status
     *         of the operation invocation. This object must not be used to try to
     *         obtain the results of the operation - the object returned from
     *         <code>Future<?>.get()</code> is implementation dependent
     *         and any use of it will result in non-portable behaviour.
     * @throws javax.xml.ws.WebServiceException
     *          If there is any error in the configuration of
     *          the <code>SourceDispatch</code> instance
     */
    public Future<?> invokeAsync(Source msg, AsyncHandler<Source> handler) {
        //return super.invokeAsync(msg, handler);
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
     * The client is responsible for ensuring that the <code>Source</code> msg
     * when marshalled is formed according to the requirements of the protocol
     * binding in use.
     *
     * @param msg An object that, when marshalled, will form the payload of
     *            the message used to invoke the operation. Must be an instance of
     *            either <code>javax.xml.transform.Source</code>.
     * @throws javax.xml.ws.WebServiceException
     *          If there is any error in the configuration of
     *          the <code>SourceDispatch</code> instance or if an error occurs
     *          during the invocation.
     */

    public void invokeOneWay(Source msg) throws
        WebServiceException {
       Message message = createMessage(msg);
       setProperties(message, Boolean.TRUE);
       Message response = process(message);
    }


    protected Message createMessage(Source msg) {
        Message message = null;
        switch (mode) {
            case PAYLOAD:
                message = new PayloadSourceMessage(msg, soapVersion);
                break;
            case MESSAGE:
                //Todo: temporary until protocol message is done
                SOAPMessage soapmsg = null;
                try {
                    //todo:
                    soapmsg = binding.getSOAPVersion().saajFactory.createMessage();
                    soapmsg.getSOAPPart().setContent(msg);
                    soapmsg.saveChanges();
                } catch (SOAPException e) {
                    throw new WebServiceException(e);
                }
                message = new SAAJMessage(soapmsg);
                break;
            default:
                throw new WebServiceException("Unrecognized message mode");
        }

        Map<String, List<String>> ch = new HashMap<String, List<String>>();

        List<String> ct = new ArrayList<String>();
        ct.add("text/xml");
        ch.put("Content-Type", ct);

        List<String> cte = new ArrayList<String>();
        cte.add("binary");
        ch.put("Content-Transfer-Encoding", cte);

        message.getProperties().httpRequestHeaders = ch;

        return message;
    }

    protected void setProperties(Message msg, boolean oneway){
        super.setProperties(msg);
        //setHttpRequestHeaders(msg);
        if (oneway)
            msg.getProperties().put(BindingProviderProperties.ONE_WAY_OPERATION,Boolean.TRUE);
    }
}