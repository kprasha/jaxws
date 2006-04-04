package com.sun.xml.ws.server;

import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractPipeImpl;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.server.provider.ProviderInvokerPipe;
import com.sun.xml.ws.server.sei.SEIInvokerPipe;
import com.sun.istack.NotNull;

import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.security.Principal;

/**
 * Base code for {@link ProviderInvokerPipe} and {@link SEIInvokerPipe}.
 *
 * <p>
 * This hides {@link InstanceResolver} and performs a set up
 * necessary for {@link WebServiceContext} to correctly.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class InvokerPipe<T> extends AbstractPipeImpl {

    private final InstanceResolver<? extends T> instanceResolver;

    protected InvokerPipe(InstanceResolver<? extends T> instanceResolver) {
        this.instanceResolver = instanceResolver;
        instanceResolver.start(webServiceContext);
    }

    /**
     * Returns the application object that serves the request.
     */
    public final @NotNull T getServant(Packet request) {
        // this allows WebServiceContext to find this packet
        packets.set(request);
        return instanceResolver.resolve(request);
    }

    /**
     * {@link InvokerPipe} is stateless and terminal, so no need to create copies.
     */
    public final Pipe copy(PipeCloner cloner) {
        cloner.add(this,this);
        return this;
    }

    public void preDestroy() {
        super.preDestroy();
        instanceResolver.dispose();
    }

    /**
     * Heart of {@link WebServiceContext}.
     * Remembers which thread is serving which packet.
     */
    private final ThreadLocal<Packet> packets = new ThreadLocal<Packet>();

    /**
     * The single {@link WebServiceContext} instance injected into application.
     */
    private final WebServiceContext webServiceContext = new WebServiceContext() {

        public MessageContext getMessageContext() {
            return new EndpointMessageContextImpl(getCurrentPacket());
        }

        public Principal getUserPrincipal() {
            Packet packet = getCurrentPacket();
            return packet.webServiceContextDelegate.getUserPrincipal(packet);
        }

        private Packet getCurrentPacket() {
            Packet p = packets.get();
            assert p!=null; // invoker must set
            return p;
        }

        public boolean isUserInRole(String role) {
            Packet packet = getCurrentPacket();
            return packet.webServiceContextDelegate.isUserInRole(packet,role);
        }
    };
}
