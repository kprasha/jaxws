/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */

package com.sun.xml.ws.client.dispatch.rearch;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.MessageProperties;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import static com.sun.xml.ws.client.BindingProviderProperties.*;
import com.sun.xml.ws.client.Stub;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.client.dispatch.DispatchContext;

import javax.xml.namespace.QName;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import java.util.Map;
import java.util.concurrent.*;

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
    protected static final long AWAIT_TERMINATION_TIME = 800L;
    private final String endpointAddress;

    /**
     *
     * @param port    dispatch instance is asssociated with this wsdl port qName
     * @param aClass  represents the class of the Message type associated with this Dispatch instance
     * @param mode    Service.mode associated with this Dispatch instance - Service.mode.MESSAGE or Service.mode.PAYLOAD
     * @param owner   Service that created the Dispatch
     * @param pipe    Master pipe for the pipeline
     * @param binding Binding of this Dispatch instance, current one of SOAP/HTTP or XML/HTTP
     */
    protected DispatchImpl(QName port, Class<T> aClass, Service.Mode mode, WSServiceDelegate owner, Pipe pipe, BindingImpl binding) {
        this(port, mode, owner, pipe, binding);
        this.clazz = aClass;
    }

    /**
     *
     * @param port    dispatch instance is asssociated with this wsdl port qName
     * @param mode    Service.mode associated with this Dispatch instance - Service.mode.MESSAGE or Service.mode.PAYLOAD
     * @param owner   Service that created the Dispatch
     * @param pipe    Master pipe for the pipeline
     * @param binding Binding of this Dispatch instance, current one of SOAP/HTTP or XML/HTTP
     */
    protected DispatchImpl(QName port, Service.Mode mode, WSServiceDelegate owner, Pipe pipe, BindingImpl binding) {
        super(pipe, binding);
        this.portname = port;
        this.mode = mode;
        this.owner = owner;
        this.soapVersion = binding.getSOAPVersion();
        this.endpointAddress = owner.getEndpointAddress(portname);
    }

    /**
     * Abstract method that is implemented by each concrete Dispatch class
     * @param msg  message passed in from the client program on the invocation
     * @return  The Message created returned as the Interface in actuallity a
     *          concrete Message Type
     */
    protected abstract Message createMessage(T msg);

    //todo: temp just to get something working


    /**
     *
     * @param param
     * @return
     */
    public Response<T> invokeAsync(final T param) {
        ResponseImpl<T> ft = new ResponseImpl<T>(new Callable() {
            public T call() throws Exception {
                return invoke(param);
            }
        });
        //executor used needs to be one set by client or default on Service
        //todo: site spec requirement
        owner.getExecutor().execute(ft);
        return ft;
    }

    /**
     *
     * @param param
     * @param asyncHandler
     * @return
     */

    /* todo: Not sure that this meets the needs of tango for async callback */
    /* todo: Need to review with team                                       */
    
    public Future<?> invokeAsync(final T param, final AsyncHandler<T> asyncHandler) {
        final ResponseImpl<T>[] r = new ResponseImpl[1];
        r[0] = new ResponseImpl<T>(new Callable() {
            public T call() throws Exception {
                T t = invoke(param);
                //doesn't work as r[0] result t has not been set
                //asynhandler will block indefinately
                //asyncHandler.handleResponse(r[0]);
                return t;
            }
        });
        r[0].setHandler(asyncHandler);
        // temp needed so that unit tests run and complete otherwise they may
        //not. Need a way to put this in the test harness or other way to do this
        //todo: as above
        ExecutorService exec = (ExecutorService) owner.getExecutor();
        try {
            exec.awaitTermination(AWAIT_TERMINATION_TIME, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        exec.execute(r[0]);
        return r[0];
    }

    /**
     *
     */
    private static class ResponseImpl<T> extends FutureTask<T> implements Response<T> {

        private AsyncHandler<T> handler;

        /**
         *
         * @param callable
         */
        protected ResponseImpl(Callable<T> callable) {
            super(callable);
        }

        /**
         *
         * @param handler
         */
        private void setHandler(AsyncHandler<T> handler) {
            this.handler = handler;
        }

        /**
         *
         */
        @Override
        protected void done() {
            if (handler == null)
                return;

            try {
                if (!isCancelled())
                    handler.handleResponse(this);
            } catch (Throwable e) {
                super.setException(e);
            } finally {
                handler = null;
            }
        }


        /**
         *
         * @return
         */
        public Map<String, Object> getContext() {
            // TODO
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @param msg
     */
    protected void setProperties(Message msg) {

        MessageProperties props = msg.getProperties();
        props.proxy = this;
        props.endpointAddress = endpointAddress;

        props.put(BINDING_ID_PROPERTY, binding.getBindingId());

        //not needed but leave for now --maybe mode is needed
        props.put(DispatchContext.DISPATCH_MESSAGE_MODE, mode);
        if (clazz != null)
            props.put(DispatchContext.DISPATCH_MESSAGE_CLASS, clazz);
        props.put("SOAPVersion", soapVersion);
        props.requestContext = getRequestContext();
    }
}