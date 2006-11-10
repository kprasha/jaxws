/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.client.dispatch;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.api.message.Attachment;
import com.sun.xml.ws.api.message.AttachmentSet;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.AsyncInvoker;
import com.sun.xml.ws.client.AsyncResponseImpl;
import com.sun.xml.ws.client.RequestContext;
import com.sun.xml.ws.client.ResponseContext;
import com.sun.xml.ws.client.ResponseContextReceiver;
import com.sun.xml.ws.client.ResponseImpl;
import com.sun.xml.ws.client.Stub;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.encoding.soap.DeserializationException;
import com.sun.xml.ws.fault.SOAPFaultBuilder;
import com.sun.xml.ws.message.AttachmentSetImpl;
import com.sun.xml.ws.message.DataHandlerAttachment;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPFaultException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    final Service.Mode mode;
    final QName portname;
    final SOAPVersion soapVersion;
    static final long AWAIT_TERMINATION_TIME = 800L;

    /**
     *
     * @param port    dispatch instance is asssociated with this wsdl port qName
     * @param mode    Service.mode associated with this Dispatch instance - Service.mode.MESSAGE or Service.mode.PAYLOAD
     * @param owner   Service that created the Dispatch
     * @param pipe    Master pipe for the pipeline
     * @param binding Binding of this Dispatch instance, current one of SOAP/HTTP or XML/HTTP
     */
    protected DispatchImpl(QName port, Service.Mode mode, WSServiceDelegate owner, Tube pipe, BindingImpl binding, WSEndpointReference epr) {
        super(owner, pipe, binding, (owner.getWsdlService() != null)? owner.getWsdlService().get(port) : null , owner.getEndpointAddress(port), epr);
        this.portname = port;
        this.mode = mode;
        this.soapVersion = binding.getSOAPVersion();
    }

    /**
     * Abstract method that is implemented by each concrete Dispatch class
     * @param msg  message passed in from the client program on the invocation
     * @return  The Message created returned as the Interface in actuallity a
     *          concrete Message Type
     */
    abstract Packet createPacket(T msg);

    /**
     * Obtains the value to return from the response message.
     */
    abstract T toReturnValue(Packet response);

    public final Response<T> invokeAsync(T param) {
        AsyncInvoker invoker = new DispatchAsyncInvoker(param);
        AsyncResponseImpl<T> ft = new AsyncResponseImpl<T>(invoker,null);
        invoker.setReceiver(ft);
        // TODO: Do we set this executor on Engine and run the AsyncInvoker in this thread ?
        owner.getExecutor().execute(ft);
        return ft;
    }

    /* todo: Not sure that this meets the needs of tango for async callback */
    /* todo: Need to review with team                                       */

    public final Future<?> invokeAsync(T param, AsyncHandler<T> asyncHandler) {
        Invoker invoker = new Invoker(param);
        ResponseImpl<T> ft = new ResponseImpl<T>(invoker,asyncHandler);
        invoker.setReceiver(ft);

        // temp needed so that unit tests run and complete otherwise they may
        //not. Need a way to put this in the test harness or other way to do this
        //todo: as above
        ExecutorService exec = (ExecutorService) owner.getExecutor();
        try {
            exec.awaitTermination(AWAIT_TERMINATION_TIME, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            throw new WebServiceException(e);
        }
        exec.execute(ft);
        return ft;
    }

    /**
     * Synchronously invokes a service.
     *
     * See {@link #process(Packet, RequestContext, ResponseContextReceiver)} on
     * why it takes a {@link RequestContext} and {@link ResponseContextReceiver} as a parameter.
     */
    public final T doInvoke(T in, RequestContext rc, ResponseContextReceiver receiver){
        Packet response;
        try {
            checkNullAllowed(in, rc, binding, mode);

            Packet message = createPacket(in);
            resolveEndpointAddress(message, rc);
            setProperties(message,true);
            response = process(message,rc,receiver);
            Message msg = response.getMessage();

            if(msg != null && msg.isFault()) {
                SOAPFaultBuilder faultBuilder = SOAPFaultBuilder.create(msg);
                // passing null means there is no checked excpetion we're looking for all
                // it will get back to us is a protocol exception
                throw (SOAPFaultException)faultBuilder.createException(null, msg);
            }
        } catch (JAXBException e) {
            //TODO: i18nify
            throw new DeserializationException("failed.to.read.response",e);
        } catch(RuntimeException e){
            //it could be a WebServiceException or a ProtocolException or any RuntimeException
            // resulting due to some internal bug.
            throw e;
        } catch(Throwable e){
            //its some other exception resulting from user error, wrap it in
            // WebServiceException
            throw new WebServiceException(e);
        }

        return toReturnValue(response);
    }

    public final T invoke(T in) {
        return doInvoke(in,requestContext,this);
    }

    public final void invokeOneWay(T in) {
        
        checkNullAllowed(in, requestContext, binding, mode);


        Packet request = createPacket(in);
        setProperties(request,false);
        Packet response = process(request,requestContext,this);
    }

    void setProperties(Packet packet, boolean expectReply) {
        packet.expectReply = expectReply;
    }

    static boolean isXMLHttp(WSBinding binding) {
        return binding.getBindingId().equals(BindingID.XML_HTTP);
    }

    static boolean isPAYLOADMode(Service.Mode mode) {
           return mode == Service.Mode.PAYLOAD;
    }

    static void checkNullAllowed(Object in, RequestContext rc, WSBinding binding, Service.Mode mode) {

        if (in != null)
            return;

        //With HTTP Binding a null invocation parameter can not be used
        //with HTTP Request Method == POST
        if (isXMLHttp(binding)){
            if (methodNotOk(rc))
                throw new WebServiceException("A XML/HTTP request using MessageContext.HTTP_REQUEST_METHOD equals \"POST\" with a Null invocation Argument is not allowed");
        } else { //soapBinding
              if (mode == Service.Mode.MESSAGE )
                  throw new WebServiceException("SOAP/HTTP Binding in Service.Mode.message is not allowed with a null invocation argument");
        }
    }

    static boolean methodNotOk(RequestContext rc) {
        String requestMethod = (String)rc.get(MessageContext.HTTP_REQUEST_METHOD);
        String request = (requestMethod == null)? "POST": requestMethod;

        return "POST".equalsIgnoreCase(request) || "PUT".equalsIgnoreCase(request);
    }

    public static void checkValidSOAPMessageDispatch(WSBinding binding, Service.Mode mode) {
        if (DispatchImpl.isXMLHttp(binding))
            throw new WebServiceException("Can not create Dispatch<SOAPMessage> with XML/HTTP Binding, SOAPBinding only.");
        if (DispatchImpl.isPAYLOADMode(mode))
            throw new WebServiceException("Can not create Dispatch<SOAPMessage> of Service.Mode.PAYLOAD, Service.Mode.MESSAGE only");
    }

    public static void checkValidDataSourceDispatch(WSBinding binding, Service.Mode mode) {
        if (!DispatchImpl.isXMLHttp(binding))
            throw new WebServiceException("Can not create Dispatch<DataSource> with SOAP Binding Binding, XML/HTTP Binding only.");
        if (DispatchImpl.isPAYLOADMode(mode))
            throw new WebServiceException("Can not create Dispatch<DataSource> of Service.Mode.PAYLOAD, Service.Mode.MESSAGE only");
    }

    protected final @NotNull QName getPortName() {
        return portname;
    }


    void resolveEndpointAddress(Packet message, RequestContext requestContext) {
        //resolve endpoint look for query parameters, pathInfo
        String origEndpoint = (String) requestContext.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);

        String pathInfo = null;
        String queryString = null;
        if (requestContext.get(MessageContext.PATH_INFO) != null) {
            pathInfo = (String) requestContext.get(MessageContext.PATH_INFO);
        }
        if (requestContext.get(MessageContext.QUERY_STRING) != null) {
            queryString = (String) requestContext.get(MessageContext.QUERY_STRING);
        }

        String resolvedEndpoint = null;
        if (pathInfo != null || queryString != null) {
            pathInfo = checkPath(pathInfo);
            queryString = checkQuery(queryString);
            if (origEndpoint != null) {
                try {
                    URI endpointURI = new URI(origEndpoint);
                    resolvedEndpoint = resolveURI(endpointURI, pathInfo, queryString);
                } catch (URISyntaxException e) {
                    resolvedEndpoint = origEndpoint;
                }
            }

            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, resolvedEndpoint);
            //message.endpointAddress = EndpointAddress.create(resolvedEndpoint);
        }
    }

    protected String resolveURI(URI endpointURI, String pathInfo, String queryString) {
        String query = null;
        String fragment = null;
        if (queryString != null) {
            URI result = endpointURI.resolve(queryString);
            query = result.getQuery();
            fragment = result.getFragment();
        }

        String path = (pathInfo != null) ? pathInfo : endpointURI.getPath();
        try {
            URI temp = new URI(null, null, path, query, fragment);
            return endpointURI.resolve(temp).toString();
        } catch (URISyntaxException e) {
            throw new WebServiceException("Unable to resolve endpoint address using the supplied path " + path);
        }
       // return endpointURI.toString();
    }

    private static String checkPath(String path) {
        //does it begin with /
        return (path == null || path.startsWith("/")) ? path : "/" + path;
    }

    private static String checkQuery(String query) {
        //does it begin with ?
        return (query == null || query.startsWith("?")) ? query : "?" + query;
    }


    protected AttachmentSet setOutboundAttachments() {
        HashMap<String, DataHandler> attachments = (HashMap<String, DataHandler>)
                getRequestContext().get(MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS);

        if (attachments != null) {
            List<Attachment> alist = new ArrayList();
            for (Map.Entry<String, DataHandler> att : attachments.entrySet()) {
                DataHandlerAttachment dha = new DataHandlerAttachment(att.getKey(), att.getValue());
                alist.add(dha);
            }
            return new AttachmentSetImpl(alist);
        }
        return new AttachmentSetImpl();
    }

   /* private void getInboundAttachments(Message msg) {
        AttachmentSet attachments = msg.getAttachments();
        if (!attachments.isEmpty()) {
            Map<String, DataHandler> in = new HashMap<String, DataHandler>();
            for (Attachment attachment : attachments)
                in.put(attachment.getContentId(), attachment.asDataHandler());
            getResponseContext().put(MessageContext.INBOUND_MESSAGE_ATTACHMENTS, in);
        }

    }
    */


    /**
     * Calls {@link DispatchImpl#doInvoke(Object,RequestContext,ResponseContextReceiver)}.
     */
    private class Invoker implements Callable {
        private final T param;
        // snapshot the context now. this is necessary to avoid concurrency issue,
        // and is required by the spec
        private final RequestContext rc = requestContext.copy();

        /**
         * Because of the object instantiation order,
         * we can't take this as a constructor parameter.
         */
        private ResponseContextReceiver receiver;

        Invoker(T param) {
            this.param = param;
        }

        public T call() throws Exception {
            return doInvoke(param,rc,receiver);
        }

        void setReceiver(ResponseContextReceiver receiver) {
            this.receiver = receiver;
        }
    }

    /**
     * 
     */
    private class DispatchAsyncInvoker extends AsyncInvoker {
        private final T param;
        // snapshot the context now. this is necessary to avoid concurrency issue,
        // and is required by the spec
        private final RequestContext rc = requestContext.copy();

        DispatchAsyncInvoker(T param) {
            this.param = param;
        }

        public void run () {
            checkNullAllowed(param, rc, binding, mode);
            Packet message = createPacket(param);
            resolveEndpointAddress(message, rc);
            setProperties(message,true);
            Fiber.CompletionCallback callback = new Fiber.CompletionCallback() {
                public void onCompletion(@NotNull Packet response) {
                    Message msg = response.getMessage();
                    try {
                        if(msg != null && msg.isFault()) {
                            SOAPFaultBuilder faultBuilder = SOAPFaultBuilder.create(msg);
                            // passing null means there is no checked excpetion we're looking for all
                            // it will get back to us is a protocol exception
                            throw (SOAPFaultException)faultBuilder.createException(null, msg);
                        }
                        responseImpl.setResponseContext(new ResponseContext(response));
                        responseImpl.set(toReturnValue(response), null);
                    } catch (JAXBException e) {
                        //TODO: i18nify
                        responseImpl.set(null, new DeserializationException("failed.to.read.response",e));
                    } catch(RuntimeException e){
                        //it could be a WebServiceException or a ProtocolException or any RuntimeException
                        // resulting due to some internal bug.
                        responseImpl.set(null, e);
                    } catch(Throwable e){
                        //its some other exception resulting from user error, wrap it in
                        // WebServiceException
                        responseImpl.set(null, new WebServiceException(e));
                    }
                }
                public void onCompletion(@NotNull Throwable error) {
                    responseImpl.set(null, error);
                }
            };
            processAsync(message,rc, callback);
        }
    }

    public void setOutboundHeaders(Object... headers) {
        throw new UnsupportedOperationException();
    }
}
