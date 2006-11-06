package com.sun.xml.ws.server;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.server.WSWebServiceContext;
import org.w3c.dom.Element;

import javax.xml.ws.EndpointReference;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import java.security.Principal;

/**
 *
 * @author Jitendra Kotamraju
 */
public abstract class AbstractWebServiceContext implements WSWebServiceContext {

    private final WSEndpoint endpoint;

    public AbstractWebServiceContext(WSEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public MessageContext getMessageContext() {
        return new EndpointMessageContextImpl(getRequestPacket());
    }

    public Principal getUserPrincipal() {
        Packet packet = getRequestPacket();
        return packet.webServiceContextDelegate.getUserPrincipal(packet);
    }

    public boolean isUserInRole(String role) {
        Packet packet = getRequestPacket();
        return packet.webServiceContextDelegate.isUserInRole(packet,role);
    }

    public EndpointReference getEndpointReference(Element...referenceParameters) {
        return getEndpointReference(W3CEndpointReference.class, referenceParameters);
    }

    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz, Element...referenceParameters) {
        Packet packet = getRequestPacket();
        String address = packet.webServiceContextDelegate.getEPRAddress(packet, endpoint);
        String wsdlAddress = null;
        if(endpoint.getServiceDefinition() != null) {
            wsdlAddress = packet.webServiceContextDelegate.getWSDLAddress(packet,endpoint);
        }
        return clazz.cast(((WSEndpointImpl)endpoint).getEndpointReference(clazz,address,wsdlAddress, referenceParameters));
    }
    
}
