package com.sun.xml.ws.transport;

import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.ClientPipeAssemblerContext;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.TransportPipeFactory;

import javax.xml.ws.BindingProvider;

/**
 * Proxy transport {@link Pipe} that lazily determines the
 * actual transport pipe by looking at {@link Packet#endpointAddress}.
 *
 * <p>
 * This pseudo transport is used when there's no statically known endpoint address,
 * and thus it's expected that the application will configure {@link BindingProvider}
 * at runtime before making invocation.
 *
 * <p>
 * Since a typical application makes multiple invocations with the same endpoint
 * address, this class implements a simple cache strategy to avoid re-creating
 * transport pipes excessively.
 *
 * @author Kohsuke Kawaguchi
 */
public final class DeferredTransportPipe implements Pipe {

    private Pipe transport;
    private EndpointAddress address;

    // parameter to TransportPipeFactory
    private final ClassLoader classLoader;
    private final ClientPipeAssemblerContext context;

    public DeferredTransportPipe(ClassLoader classLoader, ClientPipeAssemblerContext context) {
        this.classLoader = classLoader;
        this.context = context;
    }

    public Packet process(Packet request) {
        if(request.endpointAddress==address)
            // cache hit
            return transport.process(request);

        // cache miss

        if(transport!=null) {
            // delete the current entry
            transport.preDestroy();
            transport = null;
            address = null;
        }

        // otherwise find out what transport will process this.

        ClientPipeAssemblerContext newContext = new ClientPipeAssemblerContext(
            request.endpointAddress,
            context.getWsdlModel(),
            context.getService(),
            context.getBinding()
        );

        address = request.endpointAddress;
        transport = TransportPipeFactory.create(classLoader, newContext);
        // successful return from the above method indicates a successful pipe creation
        assert transport!=null;

        return transport.process(request);
    }

    public void preDestroy() {
        if(transport!=null) {
            transport.preDestroy();
            transport = null;
            address = null;
        }
    }

    public Pipe copy(PipeCloner cloner) {
        DeferredTransportPipe copy = new DeferredTransportPipe(classLoader,context);
        cloner.add(this,copy);

        // copied pipeline is still likely to work with the same endpoint address,
        // so also copy the cached transport pipe, if any
        if(transport!=null) {
            copy.transport = cloner.copy(this.transport);
            copy.address = this.address;
        }

        return copy;
    }
}
