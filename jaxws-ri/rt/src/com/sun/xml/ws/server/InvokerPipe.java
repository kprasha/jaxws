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

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.TubeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractTubeImpl;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.api.server.Invoker;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.server.provider.ProviderInvokerPipe;
import com.sun.xml.ws.server.sei.SEIInvokerPipe;

import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
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
public abstract class InvokerPipe<T> extends AbstractTubeImpl {

    private final Invoker invoker;
    private WSEndpoint endpoint;

    protected InvokerPipe(Invoker invoker) {
        this.invoker = invoker;
        invoker.start(webServiceContext);
    }

    public void setEndpoint(WSEndpoint endpoint) {
        this.endpoint = endpoint;
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
     * processRequest() and processResponse() do not share any instance variables
     * while processing the request. {@link InvokerPipe} is stateless and terminal,
     * so no need to create copies.
     */
    public final AbstractTubeImpl copy(TubeCloner cloner) {
        cloner.add(this,this);
        return this;
    }

    public void preDestroy() {
        //super.preDestroy();
        invoker.dispose();
    }

    public NextAction processRequest(Packet request) {
        Packet res = process(request);
        return doReturnWith(res);
    }

    public NextAction processResponse(Packet response) {
        throw new IllegalStateException("InovkerPipe's processResponse shouldn't be called.");
    }

    public NextAction processException(@NotNull Throwable t) {
        throw new IllegalStateException("InovkerPipe's processException shouldn't be called.");
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

        public EndpointReference getEndpointReference() {
            return getEndpointReference(W3CEndpointReference.class);
        }

        public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
            Packet packet = getCurrentPacket();
            String address = packet.webServiceContextDelegate.getEPRAddress(packet);
            return (T) ((WSEndpointImpl)endpoint).getEndpointReference(clazz,address);
        }
    };
}
