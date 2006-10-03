package com.sun.xml.ws.server.provider;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.Invoker;
import com.sun.xml.ws.fault.SOAPFaultBuilder;
import com.sun.istack.NotNull;

import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.transform.Source;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;

/**
 * This pipe is used to invoke SOAP/HTTP {@link javax.xml.ws.Provider} endpoints.
 *
 * @author Jitendra Kotamraju
 */
public class SOAPAsyncProviderInvokerPipe extends SOAPProviderInvokerPipe {

    public SOAPAsyncProviderInvokerPipe(Invoker invoker, ProviderEndpointModel model, SOAPVersion soapVersion) {
        super(invoker, model, soapVersion);
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
        return doReturnWith(response);
        //throw new IllegalStateException("InovkerPipe's processResponse shouldn't be called.");
    }

    @Override
    public NextAction processException(@NotNull Throwable t) {
        throw new IllegalStateException("InovkerPipe's processException shouldn't be called.");
    }

}
