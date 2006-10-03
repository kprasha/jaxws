package com.sun.xml.ws.api.pipe;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.helper.PipeAdapter;
import com.sun.xml.ws.api.server.ServerPipelineHook;
import com.sun.xml.ws.api.server.WSEndpoint;

/**
 * Factory for well-known server {@link Pipe} implementations
 * that the {@link PipelineAssembler} needs to use
 * to satisfy JAX-WS requirements.
 *
 * @author Jitendra Kotamraju
 * @deprecated Use {@link ServerTubeAssemblerContext}.
 */
public final class ServerPipeAssemblerContext extends ServerTubeAssemblerContext {

    public ServerPipeAssemblerContext(@Nullable SEIModel seiModel, @Nullable WSDLPort wsdlModel, @NotNull WSEndpoint endpoint, @NotNull Tube terminal, boolean isSynchronous) {
        super(seiModel, wsdlModel, endpoint, terminal, isSynchronous);
    }

    /**
     * Creates a {@link Pipe} that performs SOAP mustUnderstand processing.
     * This pipe should be before HandlerPipes.
     */
    public @NotNull Pipe createServerMUPipe(@NotNull Pipe next) {
        return PipeAdapter.adapt(super.createServerMUTube(PipeAdapter.adapt(next)));
    }

    /**
     * Creates a {@link Pipe} that does the monitoring of the invocation for a
     * container
     */
    public @NotNull Pipe createMonitoringPipe(@NotNull Pipe next) {
        ServerPipelineHook hook = endpoint.getContainer().getSPI(ServerPipelineHook.class);
        if (hook != null)
            return hook.createMonitoringPipe(this, next);
        return next;
    }

    /**
     * Creates a {@link Pipe} that adds container specific security
     */
    public @NotNull Pipe createSecurityPipe(@NotNull Pipe next) {
        ServerPipelineHook hook = endpoint.getContainer().getSPI(ServerPipelineHook.class);
        if (hook != null)
            return hook.createSecurityPipe(this, next);
        return next;
    }

    /**
     * Creates a {@link Pipe} that invokes protocol and logical handlers.
     */
    public @NotNull Pipe createHandlerPipe(@NotNull Pipe next) {
        return PipeAdapter.adapt(super.createHandlerTube(PipeAdapter.adapt(next)));
    }

    /**
     * The last {@link Pipe} in the pipeline. The assembler is expected to put
     * additional {@link Pipe}s in front of it.
     *
     * <p>
     * (Just to give you the idea how this is used, normally the terminal pipe
     * is the one that invokes the user application or {@link javax.xml.ws.Provider}.)
     *
     * @return always non-null terminal pipe
     */
     public @NotNull Pipe getTerminalPipe() {
         return PipeAdapter.adapt(terminal);
    }
}
