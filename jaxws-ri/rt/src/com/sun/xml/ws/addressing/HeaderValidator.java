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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.WebServiceException;

import static com.sun.xml.ws.addressing.W3CAddressingConstants.ONLY_ANONYMOUS_ADDRESS_SUPPORTED;
import static com.sun.xml.ws.addressing.W3CAddressingConstants.ONLY_NON_ANONYMOUS_ADDRESS_SUPPORTED;
import com.sun.xml.ws.addressing.model.InvalidMapException;
import com.sun.xml.ws.addressing.model.MapRequiredException;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.model.wsdl.WSDLBoundOperationImpl;
import com.sun.xml.ws.resources.AddressingMessages;

/**
 * @author Arun Gupta
 */
public class HeaderValidator {
    static final void checkAnonymousSemantics(WSBinding binding, WSDLBoundOperation wbo, WSEndpointReference replyTo, WSEndpointReference faultTo) throws XMLStreamException {
        // no check if Addressing is not enabled or is Member Submission
        if (binding.getAddressingVersion() == null || binding.getAddressingVersion() == AddressingVersion.MEMBER)
            return;

        if (wbo == null)
            return;

        WSDLBoundOperationImpl impl = (WSDLBoundOperationImpl)wbo;
        WSDLBoundOperationImpl.ANONYMOUS anon = impl.getAnonymous();

        AddressingVersion av = binding.getAddressingVersion();

        String replyToValue = null;
        String faultToValue = null;

        if (replyTo != null)
            replyToValue = replyTo.getAddress();

        if (faultTo != null)
            faultToValue = faultTo.getAddress();

        if (anon == WSDLBoundOperationImpl.ANONYMOUS.optional) {
            // no check is required
        } else if (anon == WSDLBoundOperationImpl.ANONYMOUS.required) {
            if (replyToValue != null && !replyToValue.equals(av.getAnonymousUri()))
                throw new InvalidMapException(av.replyToTag, ONLY_ANONYMOUS_ADDRESS_SUPPORTED);

            if (faultToValue != null && !faultToValue.equals(av.getAnonymousUri()))
                throw new InvalidMapException(av.faultToTag, ONLY_ANONYMOUS_ADDRESS_SUPPORTED);

        } else if (anon == WSDLBoundOperationImpl.ANONYMOUS.prohibited) {
            if (replyToValue != null && replyToValue.equals(av.getAnonymousUri()))
                throw new InvalidMapException(av.replyToTag, ONLY_NON_ANONYMOUS_ADDRESS_SUPPORTED);

            if (faultToValue != null && faultToValue.equals(av.getAnonymousUri()))
                throw new InvalidMapException(av.faultToTag, ONLY_NON_ANONYMOUS_ADDRESS_SUPPORTED);

        } else {
            // cannot reach here
            throw new WebServiceException(AddressingMessages.INVALID_WSAW_ANONYMOUS(anon.toString()));
        }
    }

    static final WSDLBoundOperation getWSDLBoundOperation(WSDLPort wsdlPort, Packet packet) {
        WSDLBoundOperation wbo = null;
        if (wsdlPort != null) {
            wbo = packet.getMessage().getOperation(wsdlPort);
        }
        return wbo;
    }

    static final void checkCardinality(Packet packet, WSBinding binding, WSDLPort wsdlPort) throws XMLStreamException {
        Message message = packet.getMessage();

        if (message == null)
            return;

        if (message.getHeaders() == null)
            return;

        boolean foundFrom = false;
        boolean foundTo = false;
        boolean foundReplyTo = false;
        boolean foundFaultTo = false;
        boolean foundAction = false;
        boolean foundMessageId = false;

        AddressingVersion av = binding.getAddressingVersion();
        java.util.Iterator<Header> hIter = message.getHeaders().getHeaders(av.nsUri, true);

        // no need to process if WS-A is not required and no WS-A headers are present
        if (!AddressingVersion.isRequired(binding.getFeature(av.getFeatureID())) && !hIter.hasNext())
            return;

        QName faultyHeader = null;
        WSEndpointReference replyTo = null;
        WSEndpointReference faultTo = null;

        while (hIter.hasNext()) {
            Header h = hIter.next();

            // check if the Header is in current role
            if (!isInCurrentRole(h, binding)) {
                continue;
            }

            String local = h.getLocalPart();
            if (local.equals(av.fromTag.getLocalPart())) {
                if (foundFrom) {
                    faultyHeader = av.fromTag;
                    break;
                }
                foundFrom = true;
            } else if (local.equals(av.toTag.getLocalPart())) {
                if (foundTo) {
                    faultyHeader = av.toTag;
                    break;
                }
                foundTo = true;
            } else if (local.equals(av.replyToTag.getLocalPart())) {
                if (foundReplyTo) {
                    faultyHeader = av.replyToTag;
                    break;
                }
                foundReplyTo = true;
                replyTo = h.readAsEPR(binding.getAddressingVersion());
            } else if (local.equals(av.faultToTag.getLocalPart())) {
                if (foundFaultTo) {
                    faultyHeader = av.faultToTag;
                    break;
                }
                foundFaultTo = true;
                faultTo = h.readAsEPR(binding.getAddressingVersion());
            } else if (local.equals(av.actionTag.getLocalPart())) {
                if (foundAction) {
                    faultyHeader = av.actionTag;
                    break;
                }
                foundAction = true;
            } else if (local.equals(av.messageIDTag.getLocalPart())) {
                if (foundMessageId) {
                    faultyHeader = av.messageIDTag;
                    break;
                }
                foundMessageId = true;
            } else if (local.equals(av.relatesToTag.getLocalPart())) {
                // no validation for RelatesTo
                // since there can be many
            } else if (local.equals(av.faultDetailTag.getLocalPart())) {
                // TODO: should anything be done here ?
                // TODO: fault detail element - only for SOAP 1.1
            } else {
                throw new WebServiceException(AddressingMessages.UNKNOWN_WSA_HEADER());
            }
        }

        // check for invalid cardinality first before checking
        // checking for mandatory headers
        if (faultyHeader != null) {
            throw new InvalidMapException(faultyHeader, av.invalidCardinalityTag);
        }

        // WS-A is engaged only if wsa:Action header is found
        boolean engaged = foundAction;

        // check for mandatory set of headers only if:
        // 1. WS-A is engaged or
        // 2. wsdl:required=true
        if (engaged || AddressingVersion.isRequired(binding.getFeature(av.getFeatureID()))) {
            // if no wsa:Action header is found
            if (!foundAction)
                throw new MapRequiredException(av.actionTag);

            // if no wsa:To header is found
            if (!foundTo)
                throw new MapRequiredException(av.toTag);
        }

        WSDLBoundOperation wbo = getWSDLBoundOperation(wsdlPort, packet);
        checkAnonymousSemantics(binding, wbo, replyTo, faultTo);
    }

    static final boolean isInCurrentRole(Header header, WSBinding binding) {
        // TODO: binding will be null for protocol messages
        // TODO: returning true assumes that protocol messages are
        // TODO: always in current role, this may not to be fixed.
        if (binding == null)
            return true;


        if (binding.getSOAPVersion() == SOAPVersion.SOAP_11) {
            // Rama: Why not checking for SOAP 1.1?
            return true;
        } else {
            String role = header.getRole(binding.getSOAPVersion());
            return (role.equals(SOAPVersion.SOAP_12.implicitRole));
        }
    }

}
