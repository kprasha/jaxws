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

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.fault.SOAPFaultBuilder;
import com.sun.xml.ws.message.ProblemActionHeader;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.Detail;
import javax.xml.soap.SOAPElement;
import javax.xml.ws.WebServiceException;

/**
 * An {@link EndpointMethodDispatcher} that uses
 * WS-Addressing Action Message Addressing Property, <code>wsa:Action</code>,
 * as the key for dispatching.
 * <p/>
 * A map of all wsa:Actions on the port and the corresponding {@link EndpointMethodHandler}
 * is initialized in the constructor. The wsa:Action value is extracted from
 * the request {@link Packet} and used as the key to return the correct
 * handler.
 *
 * @author Arun Gupta
 */
public class ActionBasedDispatcher implements EndpointMethodDispatcher {
    private final WSBinding binding;
    private final Map<String, EndpointMethodHandler> actionMethodHandlers;
    private String dispatchKey;

    public ActionBasedDispatcher(AbstractSEIModelImpl model, WSBinding binding, SEIInvokerTube invokerTube) {
        this.binding = binding;
        actionMethodHandlers = new HashMap<String, EndpointMethodHandler>();

        for( JavaMethodImpl m : model.getJavaMethods() ) {
            EndpointMethodHandler handler = new EndpointMethodHandler(invokerTube,model,m,binding);
            String action = null;
            if (m.getOperation() != null)
                // Could be null for an SE-based endpoint with metadata explicitly set to null
                action = m.getOperation().getOperation().getInput().getAction();
            if (action != null)
                actionMethodHandlers.put(action, handler);
        }
    }

    public EndpointMethodHandler getEndpointMethodHandler(Packet request) {
        dispatchKey = null;

        HeaderList hl = request.getMessage().getHeaders();
        if (hl == null)
            return null;

        String action = null;
        if (AddressingVersion.fromBinding(binding) != null)
            action = hl.getAction(AddressingVersion.fromBinding(binding), binding.getSOAPVersion());

        if (action == null)
            return null;

        dispatchKey = action;
        return actionMethodHandlers.get(action);
    }

    public String getDispatchKey() {
        return dispatchKey;
    }

    public String getName() {
        return "Action-based Dispatcher";
    }


    public Message getFaultMessage() {
        AddressingVersion av = AddressingVersion.fromBinding(binding);
        QName subcode = av.actionNotSupportedTag;
        String faultstring = String.format(av.actionNotSupportedText, dispatchKey);

        try {
            SOAPFault fault;
            if (binding.getSOAPVersion() == SOAPVersion.SOAP_12) {
                fault = SOAPVersion.SOAP_12.saajSoapFactory.createFault();
                fault.setFaultCode(SOAPConstants.SOAP_SENDER_FAULT);
                fault.appendFaultSubcode(subcode);
                Detail detail = fault.addDetail();
                SOAPElement se = detail.addChildElement(av.problemActionTag);
                se = se.addChildElement(av.actionTag);
                se.addTextNode(dispatchKey);
            } else {
                fault = SOAPVersion.SOAP_11.saajSoapFactory.createFault();
                fault.setFaultCode(subcode);
            }

            fault.setFaultString(faultstring);

            Message faultMessage = SOAPFaultBuilder.createSOAPFaultMessage(binding.getSOAPVersion(), fault);
            if (binding.getSOAPVersion() == SOAPVersion.SOAP_11) {
                faultMessage.getHeaders().add(new ProblemActionHeader(dispatchKey, av));
            }

            return faultMessage;
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }
}
