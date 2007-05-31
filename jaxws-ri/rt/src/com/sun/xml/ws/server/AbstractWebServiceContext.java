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
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.server;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.server.WSWebServiceContext;
import com.sun.istack.NotNull;
import org.w3c.dom.Element;

import javax.xml.ws.EndpointReference;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import java.security.Principal;

/**
 * Partial {@link WSWebServiceContext} implementation. This implementation depends on
 * {@link Packet} and concrete implementations provide it via
 * {@link #getRequestPacket()}.
 *
 * @see InvokerTube, AsyncProviderInvokerTube
 *
 * @author Jitendra Kotamraju
 */
public abstract class AbstractWebServiceContext implements WSWebServiceContext {

    private final WSEndpoint endpoint;

    public AbstractWebServiceContext(@NotNull WSEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public MessageContext getMessageContext() {
        Packet packet = getRequestPacket();
        if (packet == null) {
            throw new IllegalStateException("getMessageContext() can only be called while servicing a request");
        }
        return new EndpointMessageContextImpl(packet);
    }

    public Principal getUserPrincipal() {
        Packet packet = getRequestPacket();
        if (packet == null) {
            throw new IllegalStateException("getUserPrincipal() can only be called while servicing a request");
        }
        return packet.webServiceContextDelegate.getUserPrincipal(packet);
    }

    public boolean isUserInRole(String role) {
        Packet packet = getRequestPacket();
        if (packet == null) {
            throw new IllegalStateException("isUserInRole() can only be called while servicing a request");
        }
        return packet.webServiceContextDelegate.isUserInRole(packet,role);
    }

    public EndpointReference getEndpointReference(Element...referenceParameters) {
        return getEndpointReference(W3CEndpointReference.class, referenceParameters);
    }

    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz, Element...referenceParameters) {
        Packet packet = getRequestPacket();
        if (packet == null) {
            throw new IllegalStateException("getEndpointReference() can only be called while servicing a request");
        }
        String address = packet.webServiceContextDelegate.getEPRAddress(packet, endpoint);
        String wsdlAddress = null;
        if(endpoint.getServiceDefinition() != null) {
            wsdlAddress = packet.webServiceContextDelegate.getWSDLAddress(packet,endpoint);
        }
        return clazz.cast(((WSEndpointImpl)endpoint).getEndpointReference(clazz,address,wsdlAddress, referenceParameters));
    }
    
}
