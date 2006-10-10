package com.sun.xml.ws.server.provider;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.server.AsyncProvider;
import com.sun.xml.ws.api.server.AsyncProviderCallback;
import com.sun.xml.ws.api.server.Invoker;

import java.util.logging.Logger;

/**
 * This {@link Tube} is used to invoke the {@link AsyncProvider} endpoints.
 *
 * @author Jitendra Kotamraju
 */
public class AsyncProviderInvokerTube<T> extends ProviderInvokerTube<T> {

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.ProviderInvokerPipe");

    public AsyncProviderInvokerTube(Invoker invoker, ProviderArgumentsBuilder<T> argsBuilder) {
        super(invoker, argsBuilder);
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
        AsyncProviderInvokerTube.logger.fine("Invoking AsyncProvider Endpoint");
        try {
            // TODO WebServiceContext
            getInvoker(request).invokeAsyncProvider(request, param, callback, null);
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
            Packet packet = request.createResponse(responseMessage);
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
            Packet packet = request.createResponse(responseMessage);
            fiber.resume(packet);
        }
    }

}
