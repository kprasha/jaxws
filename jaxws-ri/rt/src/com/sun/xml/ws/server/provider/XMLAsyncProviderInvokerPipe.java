package com.sun.xml.ws.server.provider;

import com.sun.xml.ws.api.server.Invoker;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.encoding.xml.XMLMessage;
import com.sun.istack.NotNull;

import javax.xml.ws.Service;
import javax.xml.transform.Source;
import javax.activation.DataSource;

/**
 * This pipe is used to invoke XML/HTTP {@link javax.xml.ws.Provider} endpoints.
 *
 * @author Jitendra Kotamraju
 */
public class XMLAsyncProviderInvokerPipe extends XMLProviderInvokerPipe {

    public XMLAsyncProviderInvokerPipe(Invoker invoker, ProviderEndpointModel model) {
        super(invoker, model);
    }

    @Override
    public Packet process(Packet request) {
        throw new IllegalStateException("InovkerPipe's processResponse shouldn't be called.");
    }

    @Override
    public NextAction processRequest(Packet request) {
        return super.processAsyncRequest(request);
    }

    @Override
    public NextAction processResponse(Packet response) {
        throw new IllegalStateException("InovkerPipe's processResponse shouldn't be called.");
    }

    @Override
    public NextAction processException(@NotNull Throwable t) {
        throw new IllegalStateException("InovkerPipe's processException shouldn't be called.");
    }    
}
