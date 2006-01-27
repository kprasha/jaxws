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
import javax.xml.ws.Response;
import javax.xml.ws.AsyncHandler;
import java.util.concurrent.*;
import java.util.Map;

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
    private final String endpointAddress;


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
        this.endpointAddress = owner.getEndpointAddress(portname);
    }


    protected abstract Message createMessage(T msg);


    //todo: temp just to get something working

    private final Executor exec = Executors.newCachedThreadPool(); /* or whatever */

    public Response<T> invokeAsync(final T param) {
        ResponseImpl<T> ft = new ResponseImpl<T>(new Callable() {
            public T call() throws Exception {
                return invoke(param);
            }
        });

        exec.execute(ft);
        return ft;
    }

    public Future<?> invokeAsync(final T param, final AsyncHandler<T> asyncHandler) {
        final ResponseImpl<T>[] r = new ResponseImpl[1];
        r[0] = new ResponseImpl<T>(new Callable() {
            public T call() throws Exception {
                T t = invoke(param);
                asyncHandler.handleResponse(r[0]);
                return t;
            }
        });

        exec.execute(r[0]);
        return r[0];
    }

    private static class ResponseImpl<T> extends FutureTask<T> implements Response<T> {
        protected ResponseImpl(Callable<T> callable) {
            super(callable);
        }

        public Map<String, Object> getContext() {
            // TODO
            throw new UnsupportedOperationException();
        }
    }

    /**
     *
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