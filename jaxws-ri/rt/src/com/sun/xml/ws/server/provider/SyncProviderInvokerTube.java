package com.sun.xml.ws.server.provider;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.server.Invoker;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.transport.http.WSHTTPConnection;

import javax.xml.ws.http.HTTPException;
import javax.xml.ws.handler.MessageContext;
import java.util.logging.Logger;
import javax.xml.ws.Provider;

/**
 * This tube is used to invoke the {@link Provider} endpoints.
 *
 * @author Jitendra Kotamraju
 */
class SyncProviderInvokerTube<T> extends ProviderInvokerTube<T> {

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.SyncProviderInvokerTube");

    public SyncProviderInvokerTube(Invoker invoker, ProviderArgumentsBuilder<T> argsBuilder) {
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

        SyncProviderInvokerTube.logger.fine("Invoking Provider Endpoint");

        T returnValue;
        try {
            returnValue = getInvoker(request).invokeProvider(request, param);
        } catch(Exception e) {
            e.printStackTrace();
            Message responseMessage = argsBuilder.getResponseMessage(e);
            Packet response = request.createServerResponse(responseMessage,getEndpoint().getPort(),getEndpoint().getBinding());
            argsBuilder.updateResponse(response, e);
            return doReturnWith(response);
        }
        if (returnValue == null) {
            // Oneway. Send response code immediately for transports(like HTTP)
            // Don't do this above, since close() may generate some exceptions
            if (request.transportBackChannel != null) {
                request.transportBackChannel.close();
            }
            return doReturnWith(request.createServerResponse(null,getEndpoint().getPort(),getEndpoint().getBinding()));
        } else {
            return doReturnWith(request.createServerResponse(argsBuilder.getResponse(returnValue),getEndpoint().getPort(),getEndpoint().getBinding()));
        }
    }

    public NextAction processResponse(Packet response) {
        throw new IllegalStateException("InovkerPipe's processResponse shouldn't be called.");
    }

    public NextAction processException(@NotNull Throwable t) {
        throw new IllegalStateException("InovkerPipe's processException shouldn't be called.");
    }

}
