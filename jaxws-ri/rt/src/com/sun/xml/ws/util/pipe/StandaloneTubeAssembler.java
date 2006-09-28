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
        Tube head = PipeAdapter.adapt(ctxt.createTransportPipe());
        if (dump) {
            // for debugging inject a dump pipe. this is left in the production code,
            // as it would be very handy for a trouble-shooting at the production site.
            head = context.createDumpTube(head);
        }
        head = PipeAdapter.adapt(context.createWsaPipe((Pipe)head));       // TODO cleanly
        head = context.createClientMUTube(head);
        return context.createHandlerTube(head);
    }

    /**
     * On Server-side, HandlerChains cannot be changed after it is deployed.
     * During assembling the Pipelines, we can decide if we really need a
     * SOAPHandlerPipe and LogicalHandlerPipe for a particular Endpoint.
     */
    public Tube createServer(ServerTubeAssemblerContext context) {
        Tube head = context.getTerminalTube();
        head = context.createHandlerTube(head);
        head = context.createServerMUTube(head);
        head = PipeAdapter.adapt(context.createWsaPipe((Pipe)head));    // TODO cleanly
        return head;
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
