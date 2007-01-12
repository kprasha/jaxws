package com.sun.xml.ws.server.provider;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.istack.Nullable;

import javax.xml.ws.soap.SOAPBinding;

abstract class ProviderArgumentsBuilder<T> {

    /**
     * Creates a fault {@link Message} from method invocation's exception
     */
    protected abstract Message getResponseMessage(Exception e, Packet packet);

    /**
     * Creates {@link Message} from method invocation's return value
     */
    protected Packet getResponse(Packet request, Exception e, WSDLPort port, WSBinding binding) {
        Packet response = request.createServerResponse(null,port,binding);
        Message message = getResponseMessage(e, response);
        response.setMessage(message);
        updateResponse(response, e);
        return response;
    }

    /**
     * Sets various properties on the response packet.
     * <p>
     * for e.g sets a HTTP status code from method invocation's HTTPException
     */
    protected abstract void updateResponse(Packet p, Exception e);

    /**
     * Binds {@link com.sun.xml.ws.api.message.Message} to method invocation parameter
     * @param packet
     */
    protected abstract T getParameter(Packet packet);

    protected abstract Message getResponseMessage(T returnValue, Packet packet);

    /**
     * Creates {@link Message} from method invocation's return value
     */
    protected Packet getResponse(Packet request, @Nullable T returnValue, WSDLPort port, WSBinding binding) {
        Packet response = request.createServerResponse(null,port,binding);
        if (returnValue != null) {
            Message message = getResponseMessage(returnValue, response);
            response.setMessage(message);
        }
        return response;
    }

    public static ProviderArgumentsBuilder<?> create(ProviderEndpointModel model, WSBinding binding) {
        return (binding instanceof SOAPBinding) ? SOAPProviderArgumentBuilder.create(model, binding.getSOAPVersion())
                : XMLProviderArgumentBuilder.create(model);
    }

}
