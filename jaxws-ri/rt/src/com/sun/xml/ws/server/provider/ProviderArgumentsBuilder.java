package com.sun.xml.ws.server.provider;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Message;

import javax.xml.ws.soap.SOAPBinding;

abstract class ProviderArgumentsBuilder<T> {

    /**
     * Creates a fault {@link Message} from method invocation's exception
     */
    protected abstract Message getResponseMessage(Exception e);

    /**
     * Binds {@link com.sun.xml.ws.api.message.Message} to method invocation parameter
     */
    protected abstract T getParameter(Message msg);

    /**
     * Creates {@link Message} from method invocation's return value
     */
    protected abstract Message getResponse(T returnValue);

    public static ProviderArgumentsBuilder<?> create(ProviderEndpointModel model, WSBinding binding) {
        return (binding instanceof SOAPBinding) ? SOAPProviderArgumentBuilder.create(model, binding.getSOAPVersion())
                : XMLProviderArgumentBuilder.create(model);
    }

}
