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

package com.sun.xml.ws.server.sei;

import javax.xml.namespace.QName;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.util.QNameMap;
import com.sun.xml.ws.resources.ServerMessages;
import com.sun.xml.ws.fault.SOAPFaultBuilder;

/**
 * An {@link com.sun.xml.ws.server.sei.EndpointMethodDispatcher} that uses
 * SOAP payload first child's QName as the key for dispatching.
 * <p/>
 * A map of all payload QNames on the port and the corresponding {@link EndpointMethodHandler}
 * is initialized in the constructor. The payload QName is extracted from the
 * request {@link Packet} and used as the key to return the correct
 * handler.
 *
 * @author Arun Gupta
 */
public class PayloadQNameBasedDispatcher implements EndpointMethodDispatcher {
    private final QNameMap<EndpointMethodHandler> methodHandlers;
    private static final String EMPTY_PAYLOAD_LOCAL = "";
    private static final String EMPTY_PAYLOAD_NSURI = "";
    private String dispatchKey;
    private WSBinding binding;

    public PayloadQNameBasedDispatcher(AbstractSEIModelImpl model, WSBinding binding, SEIInvokerTube invokerTube) {
        this.binding = binding;
        methodHandlers = new QNameMap<EndpointMethodHandler>();
        for( JavaMethodImpl m : model.getJavaMethods() ) {
            EndpointMethodHandler handler = new EndpointMethodHandler(invokerTube,model,m,binding);
            QName payloadName = model.getQNameForJM(m);     // TODO need a new method on JavaMethodImpl
            methodHandlers.put(payloadName.getNamespaceURI(), payloadName.getLocalPart(), handler);
        }
    }

    public EndpointMethodHandler getEndpointMethodHandler(Packet request) {
        Message message = request.getMessage();
        String localPart = message.getPayloadLocalPart();
        String nsUri;
        if (localPart == null) {
            localPart = EMPTY_PAYLOAD_LOCAL;
            nsUri = EMPTY_PAYLOAD_NSURI;
        } else {
            nsUri = message.getPayloadNamespaceURI();
        }
        dispatchKey = "{" + nsUri + "}" + localPart;

        return methodHandlers.get(nsUri, localPart);
    }

    public String getDispatchKey() {
        return dispatchKey;
    }


    public String getName() {
        return "Payload QName-based Dispatcher";
    }

    public Message getFaultMessage() {
        String faultString = ServerMessages.DISPATCH_CANNOT_FIND_METHOD(dispatchKey, getName());
        return SOAPFaultBuilder.createSOAPFaultMessage(
                binding.getSOAPVersion(), faultString, binding.getSOAPVersion().faultCodeClient);
    }
}
