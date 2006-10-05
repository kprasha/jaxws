package com.sun.xml.ws.api.pipe;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.helper.PipeAdapter;
import com.sun.xml.ws.api.server.Container;

/**
 * Factory for well-known {@link Pipe} implementations
 * that the {@link PipelineAssembler} needs to use
 * to satisfy JAX-WS requirements.
 *
 * @author Kohsuke Kawaguchi
 * @deprecated Use {@link ClientTubeAssemblerContext}.
 */
public final class ClientPipeAssemblerContext extends ClientTubeAssemblerContext {

    public ClientPipeAssemblerContext(@NotNull EndpointAddress address, @NotNull WSDLPort wsdlModel, @NotNull WSService rootOwner, @NotNull WSBinding binding) {
        this(address, wsdlModel, rootOwner, binding, Container.NONE);
    }

    public ClientPipeAssemblerContext(@NotNull EndpointAddress address, @NotNull WSDLPort wsdlModel,
                                      @NotNull WSService rootOwner, @NotNull WSBinding binding,
                                      @NotNull Container container) {
        super(address, wsdlModel, rootOwner, binding, Container.NONE);
    }

    /**
     * creates a {@link Pipe} that dumps messages that pass through.
     */
    public Pipe createDumpPipe(Pipe next) {
        return PipeAdapter.adapt(super.createDumpTube(PipeAdapter.adapt(next)));
    }

    /**
     * Creates a {@link Pipe} that performs SOAP mustUnderstand processing.
     * This pipe should be before HandlerPipes.
     */
    public Pipe createClientMUPipe(Pipe next) {
        return PipeAdapter.adapt(super.createClientMUTube(PipeAdapter.adapt(next)));
    }

    /**
     * Creates a {@link Pipe} that invokes protocol and logical handlers.
     */
    public Pipe createHandlerPipe(Pipe next) {
        return PipeAdapter.adapt(super.createHandlerTube(PipeAdapter.adapt(next)));
    }

    /**
     * Creates a transport pipe (for client), which becomes the terminal pipe.
     */
    public Pipe createTransportPipe() {
        return PipeAdapter.adapt(super.createTransportTube());
    }

}
