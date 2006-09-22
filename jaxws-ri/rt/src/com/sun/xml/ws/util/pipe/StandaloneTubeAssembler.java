package com.sun.xml.ws.util.pipe;

import com.sun.xml.ws.api.pipe.*;
import com.sun.xml.ws.api.pipe.helper.PipeAdapter;
import com.sun.istack.NotNull;

/**
 * Default Pipeline assembler for JAX-WS client and server side runtimes. It
 * assembles various pipes into a pipeline that a message needs to be passed
 * through.
 *
 * @author Jitendra Kotamraju
 */
public class StandaloneTubeAssembler implements TubelineAssembler {

    @NotNull
    public Tube createClient(ClientTubeAssemblerContext context) {
        ClientPipeAssemblerContext ctxt = new ClientPipeAssemblerContext(
                context.getAddress(), context.getWsdlModel(), context.getService(),
                context.getBinding(), context.getContainer());
        return PipeAdapter.adapt(new StandalonePipeAssembler().createClient(ctxt));
    }

    /**
     * On Server-side, HandlerChains cannot be changed after it is deployed.
     * During assembling the Pipelines, we can decide if we really need a
     * SOAPHandlerPipe and LogicalHandlerPipe for a particular Endpoint.
     */
    public Tube createServer(ServerTubeAssemblerContext context) {
        ServerPipeAssemblerContext ctxt = new ServerPipeAssemblerContext(
                context.getSEIModel(), context.getWsdlModel(), context.getEndpoint(),
                context.getTerminalPipe(), context.isSynchronous());
        return PipeAdapter.adapt(new StandalonePipeAssembler().createServer(ctxt));
    }

    /**
     * Are we going to dump the message to System.out?
     */
    private static final boolean dump;

    static {
        boolean b = false;
        try {
            b = Boolean.getBoolean(StandaloneTubeAssembler.class.getName()+".dump");
        } catch (Throwable t) {
            // treat it as false
        }
        dump = b;
    }
}
