package com.sun.xml.ws.api.pipe;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.addressing.MemberSubmissionAddressingFeature;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.util.pipe.DumpPipe;
import com.sun.xml.ws.util.pipe.DumpTube;
import com.sun.xml.ws.handler.HandlerPipe;
import com.sun.xml.ws.handler.ClientSOAPHandlerPipe;
import com.sun.xml.ws.handler.ClientLogicalHandlerPipe;
import com.sun.xml.ws.transport.DeferredTransportPipe;
import com.sun.xml.ws.protocol.soap.ClientMUPipe;
import com.sun.xml.ws.addressing.WsaClientPipe;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;

import javax.xml.ws.soap.SOAPBinding;

/**
 * Factory for well-known {@link Tube} implementations
 * that the {@link TubelineAssembler} needs to use
 * to satisfy JAX-WS requirements.
 *
 * @author Jitendra Kotamraju
 */
public final class ClientTubeAssemblerContext {
    private final @NotNull EndpointAddress address;
    private final @NotNull WSDLPort wsdlModel;
    private final @NotNull WSService rootOwner;
    private final @NotNull WSBinding binding;
    private final @NotNull Container container;

    public ClientTubeAssemblerContext(@NotNull EndpointAddress address, @NotNull WSDLPort wsdlModel, @NotNull WSService rootOwner, @NotNull WSBinding binding) {
        this(address, wsdlModel, rootOwner, binding, Container.NONE);
    }

    public ClientTubeAssemblerContext(@NotNull EndpointAddress address, @NotNull WSDLPort wsdlModel,
                                      @NotNull WSService rootOwner, @NotNull WSBinding binding,
                                      @NotNull Container container) {
        this.address = address;
        this.wsdlModel = wsdlModel;
        this.rootOwner = rootOwner;
        this.binding = binding;
        this.container = container;
    }

    /**
     * The endpoint address. Always non-null. This parameter is taken separately
     * from {@link com.sun.xml.ws.api.model.wsdl.WSDLPort} (even though there's {@link com.sun.xml.ws.api.model.wsdl.WSDLPort#getAddress()})
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
     * The pipeline is created for this {@link com.sun.xml.ws.api.WSService}.
     * Always non-null. (To be precise, the newly created pipeline
     * is owned by a proxy or a dispatch created from thsi {@link com.sun.xml.ws.api.WSService}.)
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
     * Returns the Container in which the client is running
     *
     * @return Container in which client is running
     */
    public Container getContainer() {
        return container;
    }

    /**
     * creates a {@link Tube} that dumps messages that pass through.
     */
    public Tube createDumpTube(Tube next) {
        return new DumpTube("dump", System.out, next);
    }

    /**
     * Creates a {@link Tube} that invokes protocol and logical handlers.
     */
    public Tube createHandlerTube(Tube next) {
        HandlerPipe soapHandlerPipe = null;
        //XML/HTTP Binding can have only LogicalHandlerPipe
        if (binding instanceof SOAPBinding) {
            soapHandlerPipe = new ClientSOAPHandlerPipe(binding, wsdlModel, next);
            next = soapHandlerPipe;
        }
        return new ClientLogicalHandlerPipe(binding, next, soapHandlerPipe);
    }

    /**
     * Creates a {@link Tube} that performs SOAP mustUnderstand processing.
     * This pipe should be before HandlerPipes.
     */
    public Tube createClientMUTube(Tube next) {
        if(binding instanceof SOAPBinding)
            return new ClientMUPipe(binding,next);
        else
            return next;
    }

    /**
     * Creates WS-Addressing pipe
     */
    public Pipe createWsaPipe(Pipe next) {
        if (wsdlModel == null) {
            if (binding.hasFeature(MemberSubmissionAddressingFeature.ID)) //||
                    //ToDO: AddreassingFeature check
                    //binding.
                return new WsaClientPipe(wsdlModel, binding, next);
            else
                return next;
        }
        WSDLPortImpl impl = (WSDLPortImpl)wsdlModel;
        if (impl.isAddressingEnabled())
            return new WsaClientPipe(wsdlModel, binding, next);
        else
            return next;
    }

}
