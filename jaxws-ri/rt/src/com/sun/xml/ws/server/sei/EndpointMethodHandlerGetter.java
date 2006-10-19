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

import java.util.Map;
import java.util.HashMap;

import javax.xml.namespace.QName;

import com.sun.xml.ws.util.QNameMap;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;

/**
 * Gets the {@link EndpointMethodHandler} from a {@link Packet}. Uses
 * {@link Message} payload's QName to obtain the handler. If no handler is
 * registered corresponding to that QName, then uses Action Message
 * Addressing Property value to get the handler. 
 *
 * @author Arun Gupta
 */
public class EndpointMethodHandlerGetter {
    private final WSBinding binding;
    private final QNameMap<EndpointMethodHandler> methodHandlers;
    private final Map<String, EndpointMethodHandler> actionMethodHandlers;
    private static final String EMPTY_PAYLOAD_LOCAL = "";
    private static final String EMPTY_PAYLOAD_NSURI = "";
    private String nsUri;
    private String localPart;

    EndpointMethodHandlerGetter(AbstractSEIModelImpl model, WSBinding binding, SEIInvokerTube invokerTube) {
        this.binding = binding;

        methodHandlers = new QNameMap<EndpointMethodHandler>();
        actionMethodHandlers = new HashMap<String, EndpointMethodHandler>();
        // fill in methodHandlers.
        for( JavaMethodImpl m : model.getJavaMethods() ) {
            EndpointMethodHandler handler = new EndpointMethodHandler(invokerTube,model,m,binding);
            QName payloadName = model.getQNameForJM(m);     // TODO need a new method on JavaMethodImpl
            String action = null;
            if (m.getOperation() != null)
                // todo: check when could it be null ?
                action = m.getOperation().getOperation().getInput().getAction();
            if (action != null)
                actionMethodHandlers.put(action, handler);
            methodHandlers.put(payloadName.getNamespaceURI(), payloadName.getLocalPart(), handler);
        }
    }

    EndpointMethodHandler getEndpointMethodHandler(Packet request) {
        // get Message payload-based handler
        EndpointMethodHandler handler = getPayloadBasedHandler(request);

        if (handler != null)
            return handler;

        // get Action-based handler
        return getActionBasedHandler(request);
    }

    private EndpointMethodHandler getActionBasedHandler(Packet request) {
        String action = null;
        
        HeaderList hl = request.getMessage().getHeaders();
        if (hl != null) {
            if (binding.getAddressingVersion() != null)
                action = hl.getAction(binding.getAddressingVersion(), binding.getSOAPVersion());
        }
        if (action != null)
            return actionMethodHandlers.get(action);

        return null;
    }

    private EndpointMethodHandler getPayloadBasedHandler(Packet request) {
        Message message = request.getMessage();
        localPart = message.getPayloadLocalPart();
        if (localPart == null) {
            localPart = EMPTY_PAYLOAD_LOCAL;
            nsUri = EMPTY_PAYLOAD_NSURI;
        } else {
            nsUri = message.getPayloadNamespaceURI();
        }

        return methodHandlers.get(nsUri, localPart);
    }

    String getPayloadNamespaceURI() {
        return nsUri;
    }

    String getPayloadLocalPart() {
        return localPart;
    }
}
