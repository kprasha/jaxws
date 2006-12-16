package com.sun.xml.ws.server.provider;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.server.AsyncProvider;
import com.sun.xml.ws.api.server.AsyncProviderCallback;
import com.sun.xml.ws.api.server.Invoker;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.server.AbstractWebServiceContext;

import java.util.logging.Logger;

/**
 * This {@link Tube} is used to invoke the {@link AsyncProvider} endpoints.
 *
 * @author Jitendra Kotamraju
 */
class AsyncProviderInvokerTube<T> extends ProviderInvokerTube<T> {

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.AsyncProviderInvokerTube");

    public AsyncProviderInvokerTube(Invoker invoker, ProviderArgumentsBuilder<T> argsBuilder) {
        super(invoker, argsBuilder);
    }

   /*
    * This binds the parameter for Provider endpoints and invokes the
    * invoke() method of {@linke Provider} endpoint. The return value from
    * invoke() is used to create a new {@link Message} that traverses
    * through the Pipeline to transport.
    */
    public @NotNull NextAction processRequest(@NotNull Packet request) {
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

        public void send(@Nullable T param) {
            Message responseMessage;
            if (param == null) {
                if (request.transportBackChannel != null) {
                    request.transportBackChannel.close();
                }
                responseMessage = null;
            } else {
                responseMessage = argsBuilder.getResponse(param);
            }
            Packet packet = request.createServerResponse(responseMessage,getEndpoint().getPort(),getEndpoint().getBinding());
            fiber.resume(packet);
        }

        public void sendError(@NotNull Throwable t) {
            Exception e;
            if (t instanceof RuntimeException) {
                e = (RuntimeException)t;
            } else {
                e = new RuntimeException(t);
            }
            Message responseMessage = argsBuilder.getResponseMessage(e);
            Packet packet = request.createServerResponse(responseMessage,getEndpoint().getPort(),getEndpoint().getBinding());
            fiber.resume(packet);
        }
    }

    /**
     * The single {@link javax.xml.ws.WebServiceContext} instance injected into application.
     */
    private static final class AsyncWebServiceContext extends AbstractWebServiceContext {
        final Packet packet;

        AsyncWebServiceContext(WSEndpoint endpoint, Packet packet) {
            super(endpoint);
            this.packet = packet;
        }

        public @NotNull Packet getRequestPacket() {
            return packet;
        }
    }

    public @NotNull NextAction processResponse(@NotNull Packet response) {
        return doReturnWith(response);
    }

    public @NotNull NextAction processException(@NotNull Throwable t) {
        throw new IllegalStateException("AsyncProviderInvokerTube's processException shouldn't be called.");
    }

}
