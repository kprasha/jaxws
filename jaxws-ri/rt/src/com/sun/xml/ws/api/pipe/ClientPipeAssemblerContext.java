package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.handler.*;
import com.sun.xml.ws.protocol.soap.ClientMUPipe;
import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.util.pipe.DumpPipe;
import com.sun.xml.ws.transport.DeferredTransportPipe;

import javax.xml.ws.soap.SOAPBinding;

/**
 * Factory for well-known {@link Pipe} implementations
 * that the {@link PipelineAssembler} needs to use
 * to satisfy JAX-WS requirements.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ClientPipeAssemblerContext {
    private final @NotNull EndpointAddress address;
    private final @NotNull WSDLPort wsdlModel;
    private final @NotNull WSService rootOwner;
    private final @NotNull WSBinding binding;

    public ClientPipeAssemblerContext(@NotNull EndpointAddress address, @NotNull WSDLPort wsdlModel, @NotNull WSService rootOwner, @NotNull WSBinding binding) {
        this.address = address;
        this.wsdlModel = wsdlModel;
        this.rootOwner = rootOwner;
        this.binding = binding;
    }

    /**
     * The endpoint address. Always non-null. This parameter is taken separately
     * from {@link WSDLPort} (even though there's {@link WSDLPort#getAddress()})
     * because sometimes WSDL is not available.
     */
    public @NotNull EndpointAddress getAddress() {
        return address;
    }

    /**
     * The created pipeline will be used to serve this port.
     * Null if the service isn't associated with any port definition in WSDL,
     * and otherwise non-null.
     */
    public @Nullable WSDLPort getWsdlModel() {
        return wsdlModel;
    }

    /**
     * The pipeline is created for this {@link WSService}.
     * Always non-null. (To be precise, the newly created pipeline
     * is owned by a proxy or a dispatch created from thsi {@link WSService}.)
     */
    public @NotNull WSService getService() {
        return rootOwner;
    }

    /**
     * The binding of the new pipeline to be created.
     */
    public @NotNull WSBinding getBinding() {
        return binding;
    }

    /**
     * creates a {@link Pipe} that dumps messages that pass through.
     */
    public Pipe createDumpPipe(Pipe next) {
        return new DumpPipe("dump", System.out, next);
    }
    
    /**
     * Creates a {@link Pipe} that performs SOAP mustUnderstand processing.
     * This pipe should be before HandlerPipes.
     */
    public Pipe createClientMUPipe(Pipe next) {
        if(binding instanceof SOAPBinding)
            return new ClientMUPipe(binding,next);
        else
            return next;
    }
    
    /**
     * Creates a {@link Pipe} that invokes protocol and logical handlers.
     */
    public Pipe createHandlerPipe(Pipe next) {
        HandlerPipe soapHandlerPipe = null;
        //XML/HTTP Binding can have only LogicalHandlerPipe
        if (binding instanceof SOAPBinding) {
            soapHandlerPipe = new ClientSOAPHandlerPipe(binding, wsdlModel, next);
            next = soapHandlerPipe;
        }
        return new ClientLogicalHandlerPipe(binding, next, soapHandlerPipe);
    }
    
    /**
     * Creates a transport pipe (for client), which becomes the terminal pipe.
     */
    public Pipe createTransportPipe() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        // wsgen generates a WSDL with the address attribute that says "REPLACE_WITH_ACTUAL_URL".
        // while it's technically correct to reject such address (since there's no transport registered
        // with it), it's desirable to allow the user a benefit of doubt, and wait until the runtime
        // to see if the user configures the endpoint address through request context.
        // DeferredTransportPipe is used for this purpose.
        //
        // Ideally, we shouldn't have @address at all for such cases, but due to the backward
        // compatibility and the fact that this attribute is mandatory, we have no option but
        // to check for REPLACE_WITH_ACTUAL_URL.
        if(address.toString().equals("") || address.toString().equals("REPLACE_WITH_ACTUAL_URL"))
            return new DeferredTransportPipe(cl,this);

        return TransportPipeFactory.create(cl, this);
    }

}
