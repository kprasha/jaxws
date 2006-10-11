package com.sun.xml.ws.server.provider;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.server.*;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.server.EndpointMessageContextImpl;
import com.sun.xml.ws.server.WSEndpointImpl;

import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import java.util.logging.Logger;
import java.security.Principal;

import org.w3c.dom.Element;

/**
 * This {@link Tube} is used to invoke the {@link AsyncProvider} endpoints.
 *
 * @author Jitendra Kotamraju
 */
class AsyncProviderInvokerTube<T> extends ProviderInvokerTube<T> {

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.AsyncProviderInvokerTube");

    public AsyncProviderInvokerTube(Invoker invoker, ProviderArgumentsBuilder<T> argsBuilder, WSBinding binding) {
        super(invoker, argsBuilder, binding);
    }

   /*
    * This binds the parameter for Provider endpoints and invokes the
    * invoke() method of {@linke Provider} endpoint. The return value from
    * invoke() is used to create a new {@link Message} that traverses
    * through the Pipeline to transport.
    */
    public NextAction processRequest(Packet request) {
        T param = argsBuilder.getParameter(request.getMessage());
        AsyncProviderCallback callback = new AsyncProviderInvokerTube.AsyncProviderCallbackImpl(request);
        AsyncWebServiceContext ctxt = new AsyncWebServiceContext(getEndpoint(),request);

        AsyncProviderInvokerTube.logger.fine("Invoking AsyncProvider Endpoint");
        try {
            getInvoker(request).invokeAsyncProvider(request, param, callback, ctxt);
        } catch(Exception e) {
            e.printStackTrace();
            return doThrow(e);
        }
        // Suspend the Fiber. AsyncProviderCallback will resume the Fiber after
        // it receives response.
        return doSuspend();
    }

    private class AsyncProviderCallbackImpl implements AsyncProviderCallback<T> {
        private final Packet request;
        private final Fiber fiber;

        public AsyncProviderCallbackImpl(Packet request) {
            this.request = request;
            this.fiber = Fiber.current();
        }

        public void send(T param) {
            Message responseMessage = argsBuilder.getResponse(param);
            Packet packet = request.createServerResponse(responseMessage,null,binding);
            fiber.resume(packet);
        }

        public void sendError(Throwable t) {
            Exception e;
            if (t instanceof RuntimeException) {
                e = (RuntimeException)t;
            } else {
                e = new RuntimeException(t);
            }
            Message responseMessage = argsBuilder.getResponseMessage(e);
            Packet packet = request.createServerResponse(responseMessage,null,binding);
            fiber.resume(packet);
        }
    }

    /**
     * The single {@link javax.xml.ws.WebServiceContext} instance injected into application.
     */
    private static final class AsyncWebServiceContext implements WSWebServiceContext {

        final Packet packet;
        final WSEndpoint endpoint;

        AsyncWebServiceContext(WSEndpoint endpoint, Packet packet) {
            this.packet = packet;
            this.endpoint = endpoint;
        }

        public MessageContext getMessageContext() {
            return new EndpointMessageContextImpl(getRequestPacket());
        }

        public Principal getUserPrincipal() {
            return packet.webServiceContextDelegate.getUserPrincipal(packet);
        }

        public @NotNull Packet getRequestPacket() {
            return packet;
        }

        public boolean isUserInRole(String role) {
            Packet packet = getRequestPacket();
            return packet.webServiceContextDelegate.isUserInRole(packet,role);
        }

        public EndpointReference getEndpointReference(Element...referenceParameters) {
            return getEndpointReference(W3CEndpointReference.class, referenceParameters);
        }

        public <T extends EndpointReference> T getEndpointReference(Class<T> clazz, Element...referenceParameters) {
            Packet packet = getRequestPacket();
            String address = packet.webServiceContextDelegate.getEPRAddress(packet, endpoint);
            return (T) ((WSEndpointImpl)endpoint).getEndpointReference(clazz,address);
        }
    }

    public NextAction processResponse(Packet response) {
        return doReturnWith(response);
    }

    public NextAction processException(@NotNull Throwable t) {
        throw new IllegalStateException("AsyncProviderInvokerTube's processException shouldn't be called.");
    }

}
