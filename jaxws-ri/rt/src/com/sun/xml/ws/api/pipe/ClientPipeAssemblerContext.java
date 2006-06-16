package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.protocol.soap.ClientMUPipe;
import com.sun.istack.NotNull;
import com.sun.istack.Nullable;

import javax.xml.ws.soap.SOAPBinding;

/**
 * Factory for well-known {@link Pipe} implementations
 * that the {@link PipelineAssembler} needs to use
 * to satisfy JAX-WS requirements.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ClientPipeAssemblerContext {
    private final EndpointAddress address;
    private final WSDLPort wsdlModel;
    private final WSService rootOwner;
    private final WSBinding binding;

    public ClientPipeAssemblerContext(EndpointAddress address, WSDLPort wsdlModel, WSService rootOwner, WSBinding binding) {
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
    public @NotNull WSService getRootOwner() {
        return rootOwner;
    }

    /**
     * The binding of the new pipeline to be created.
     */
    public @NotNull WSBinding getBinding() {
        return binding;
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

    ///**
    // * Creates a {@link Pipe} that performs SOAP mustUnderstand processing.
    // *
    // * This pipe has to be placed before handler pipes,
    // * and by the time this pipe sees a {@link Message},
    // * headers need to be marked as "understood".
    // */
    //public static Pipe createServerMUPipe(@Nullable SEIModel seiModel, @Nullable WSDLPort wsdlModel, @NotNull WSEndpoint owner, @NotNull Pipe next) {
    //    return new ServerMUPipe(owner.getBinding(),next);
    //}
}
