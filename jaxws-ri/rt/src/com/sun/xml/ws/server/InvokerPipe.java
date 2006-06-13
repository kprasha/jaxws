/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.server;

import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractPipeImpl;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.api.server.Invoker;
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

    private final Invoker invoker;

    protected InvokerPipe(Invoker instanceResolver) {
        this.invoker = instanceResolver;
        instanceResolver.start(webServiceContext);
    }

    /**
     * Returns the application object that serves the request.
     *
    public final @NotNull T getServant(Packet request) {
        // this allows WebServiceContext to find this packet
        packets.set(request);
        return invoker.resolve(request);
    }
     */
    
    /**
     * Returns the {@link Invoker} object that serves the request.
     */
    public final @NotNull Invoker getInvoker(Packet request) {
        // this allows WebServiceContext to find this packet
        packets.set(request);
        return invoker;
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
        invoker.dispose();
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
