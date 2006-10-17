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

package com.sun.xml.ws.addressing;

import javax.xml.ws.WebServiceException;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLOperation;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.TubeCloner;
import com.sun.xml.ws.addressing.model.ActionNotSupportedException;
import com.sun.xml.ws.resources.AddressingMessages;

/**
 * @author Arun Gupta
 */
public class WsaClientPipe extends WsaPipe {
    public WsaClientPipe(WSDLPort wsdlPort, WSBinding binding, Tube next) {
        super(wsdlPort, binding, next);
    }

    public WsaClientPipe(WsaClientPipe that, TubeCloner cloner) {
        super(that, cloner);
    }

    public WsaClientPipe copy(TubeCloner cloner) {
        return new WsaClientPipe(this, cloner);
    }

    public @NotNull NextAction processRequest(Packet request) {
        if(wsdlPort == null) {
            // Addressing is not enabled
            return doInvoke(next,request);
        }
        if (AddressingVersion.fromBinding(binding) != null) {
            // populate request WS-Addressing headers
            HeaderList headerList = request.getMessage().getHeaders();
            headerList.fillRequestAddressingHeaders(wsdlPort, binding, request);
//            if(endpointReference!=null)
//                endpointReference.addReferenceParameters(headerList);
        }
        return doInvoke(next,request);
   }

    public @NotNull NextAction processResponse(Packet response) {
        // if one-way then, no validation
        if (response.getMessage() != null)
            response = validateInboundHeaders(response);

        return doReturnWith(response);
    }

    @Override
    public void validateAction(Packet packet) {
        //There may not be a WSDL operation.  There may not even be a WSDL.
        //For instance this may be a RM CreateSequence message.
        WSDLBoundOperation wbo = getWSDLBoundOperation(packet);

        WSDLOperation op = null;

        if (wbo != null) {
            op = wbo.getOperation();
        }

        if (wbo == null || op == null) {
            return;
        }

        String gotA = packet.getMessage().getHeaders().getAction(binding.getAddressingVersion(), binding.getSOAPVersion());

        if (gotA == null)
            throw new WebServiceException(AddressingMessages.VALIDATION_CLIENT_NULL_ACTION());

        String expected = helper.getOutputAction(packet);

        if (expected != null && !gotA.equals(expected)) {
            throw new ActionNotSupportedException(gotA);
        }
    }
}
