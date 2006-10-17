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

import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.WebServiceException;
import javax.xml.soap.SOAPFault;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import com.sun.xml.ws.api.pipe.helper.AbstractFilterTubeImpl;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.TubeCloner;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.developer.MemberSubmissionAddressingFeature;
import com.sun.xml.ws.message.FaultDetailHeader;
import com.sun.xml.ws.addressing.model.InvalidMapException;
import com.sun.xml.ws.addressing.model.MapRequiredException;
import com.sun.xml.ws.addressing.model.ActionNotSupportedException;
import static com.sun.xml.ws.addressing.W3CAddressingConstants.ONLY_ANONYMOUS_ADDRESS_SUPPORTED;
import static com.sun.xml.ws.addressing.W3CAddressingConstants.ONLY_NON_ANONYMOUS_ADDRESS_SUPPORTED;
import com.sun.xml.ws.resources.AddressingMessages;
import com.sun.xml.ws.model.wsdl.WSDLBoundOperationImpl;
import com.sun.istack.NotNull;

/**
 * @author Arun Gupta
 */
public abstract class WsaTube extends AbstractFilterTubeImpl {
    protected final WSDLPort wsdlPort;
    protected final WSBinding binding;
    final WsaTubeHelper helper;

    public WsaTube(WSDLPort wsdlPort, WSBinding binding, Tube next) {
        super(next);
        this.wsdlPort = wsdlPort;
        this.binding = binding;
        helper = getTubeHelper();
    }

    public WsaTube(WsaTube that, TubeCloner cloner) {
        super(that, cloner);
        this.wsdlPort = that.wsdlPort;
        this.binding = that.binding;
        this.helper = that.helper;
    }

    public void preDestroy() {
        // No resources to clean up
    }

    @Override
    public
    @NotNull
    NextAction processException(Throwable t) {
        return super.processException(t);
    }

    protected WsaTubeHelper getTubeHelper() {
        if(binding.isFeatureEnabled(AddressingFeature.ID)) {
            return new WsaTubeHelperImpl(wsdlPort, binding);
        } else if(binding.isFeatureEnabled(MemberSubmissionAddressingFeature.ID)) {
            return new com.sun.xml.ws.addressing.v200408.WsaTubeHelperImpl(wsdlPort, binding);
        } else {
            // Addressing is not enabled, WsaTube should not be included in the pipeline
            throw new WebServiceException(AddressingMessages.ADDRESSING_NOT_ENABLED(this.getClass().getSimpleName()));
        }
    }

    public final Packet validateInboundHeaders(Packet packet) {
        SOAPFault soapFault;
        FaultDetailHeader s11FaultDetailHeader;

        try {
            checkCardinality(packet, binding, wsdlPort);

            if (isAddressingEngagedOrRequired(packet, binding)) {
                validateAction(packet);
            }

            return packet;
        } catch (InvalidMapException e) {
            soapFault = helper.newInvalidMapFault(e, binding.getAddressingVersion());
            s11FaultDetailHeader = new FaultDetailHeader(binding.getAddressingVersion(), binding.getAddressingVersion().problemHeaderQNameTag.getLocalPart(), e.getMapQName());
        } catch (MapRequiredException e) {
            soapFault = helper.newMapRequiredFault(e, binding.getAddressingVersion());
            s11FaultDetailHeader = new FaultDetailHeader(binding.getAddressingVersion(), binding.getAddressingVersion().problemHeaderQNameTag.getLocalPart(), e.getMapQName());
        } catch (ActionNotSupportedException e) {
            soapFault = helper.newActionNotSupportedFault(e.getAction(), binding.getAddressingVersion());
            s11FaultDetailHeader = new FaultDetailHeader(binding.getAddressingVersion(), binding.getAddressingVersion().problemHeaderQNameTag.getLocalPart(), e.getAction());
        }

        if (soapFault != null) {
            // WS-A fault processing for one-way methods
            if (packet.getMessage().isOneWay(wsdlPort)) {
                return packet.createServerResponse(null, wsdlPort, binding);
            }

            Message m = Messages.create(soapFault);
            if (binding.getSOAPVersion() == SOAPVersion.SOAP_11) {
                m.getHeaders().add(s11FaultDetailHeader);
            }

            Packet response = packet.createServerResponse(m, wsdlPort, binding);
            return response;
        }

        return packet;
    }

    final boolean isAddressingEngagedOrRequired(Packet packet, WSBinding binding) {
        if (AddressingVersion.isRequired(binding))
            return true;

        if (packet == null)
            return false;

        if (packet.getMessage() == null)
            return false;

        if (packet.getMessage().getHeaders() != null)
            return false;

        String action = packet.getMessage().getHeaders().getAction(binding.getAddressingVersion(), binding.getSOAPVersion());
        if (action == null)
            return true;

        return true;
    }

    /**
     * Checks the cardinality of WS-Addressing headers on an inbound {@link Packet}. This method
     * checks for the cardinality if WS-Addressing is engaged (detected by the presence of wsa:Action
     * header) or wsdl:required=true.
     *
     * @param packet The inbound packet.
     * @param binding The Binding.
     * @param wsdlPort The WSDL port.
     *
     * @throws WebServiceException if:
     * <ul>
     * <li>there is an error reading ReplyTo or FaultTo</li>
     * <li>WS-Addressing is required and {@link Message} within <code>packet</code> is null</li>
     * <li>WS-Addressing is required and no headers are found in the {@link Message}</li>
     * <li>an uknown WS-Addressing header is present</li>
     * </ul>
     */

    public final void checkCardinality(Packet packet, WSBinding binding, WSDLPort wsdlPort) {
        Message message = packet.getMessage();
        AddressingVersion av = binding.getAddressingVersion();

        if (message == null) {
            if (AddressingVersion.isRequired(binding.getFeature(av.getFeatureID())))
                throw new WebServiceException(AddressingMessages.NULL_MESSAGE());
            else
                return;
        }

        if (message.getHeaders() == null) {
            if (AddressingVersion.isRequired(binding.getFeature(av.getFeatureID())))
                throw new WebServiceException(AddressingMessages.NULL_HEADERS());
            else
                return;
        }

        java.util.Iterator<Header> hIter = message.getHeaders().getHeaders(av.nsUri, true);

        if (!hIter.hasNext()) {
            // no WS-A headers are found

            if (AddressingVersion.isRequired(binding.getFeature(av.getFeatureID())))
                // if WS-A is required, then throw an exception looking for wsa:Action header
                throw new InvalidMapException(av.actionTag, av.invalidCardinalityTag);
            else
                // else no need to process
                return;
        }

        boolean foundFrom = false;
        boolean foundTo = false;
        boolean foundReplyTo = false;
        boolean foundFaultTo = false;
        boolean foundAction = false;
        boolean foundMessageId = false;
        QName duplicateHeader = null;
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
                    duplicateHeader = av.fromTag;
                    break;
                }
                foundFrom = true;
            } else if (local.equals(av.toTag.getLocalPart())) {
                if (foundTo) {
                    duplicateHeader = av.toTag;
                    break;
                }
                foundTo = true;
            } else if (local.equals(av.replyToTag.getLocalPart())) {
                if (foundReplyTo) {
                    duplicateHeader = av.replyToTag;
                    break;
                }
                foundReplyTo = true;
                try {
                    replyTo = h.readAsEPR(binding.getAddressingVersion());
                } catch (XMLStreamException e) {
                    throw new WebServiceException(AddressingMessages.REPLY_TO_CANNOT_PARSE(), e);
                }
            } else if (local.equals(av.faultToTag.getLocalPart())) {
                if (foundFaultTo) {
                    duplicateHeader = av.faultToTag;
                    break;
                }
                foundFaultTo = true;
                try {
                    faultTo = h.readAsEPR(binding.getAddressingVersion());
                } catch (XMLStreamException e) {
                    throw new WebServiceException(AddressingMessages.FAULT_TO_CANNOT_PARSE(), e);
                }
            } else if (local.equals(av.actionTag.getLocalPart())) {
                if (foundAction) {
                    duplicateHeader = av.actionTag;
                    break;
                }
                foundAction = true;
            } else if (local.equals(av.messageIDTag.getLocalPart())) {
                if (foundMessageId) {
                    duplicateHeader = av.messageIDTag;
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

        // check for invalid cardinality first before checking for mandatory headers
        if (duplicateHeader != null) {
            throw new InvalidMapException(duplicateHeader, av.invalidCardinalityTag);
        }

        // WS-A is engaged if wsa:Action header is found
        boolean engaged = foundAction;

        // check for mandatory set of headers only if:
        // 1. WS-A is engaged or
        // 2. wsdl:required=true
        // Both wsa:Action and wsa:To MUST be present on request (for oneway MEP) and
        // response messages (for oneway and request/response MEP only)
        if (engaged || AddressingVersion.isRequired(binding.getFeature(av.getFeatureID()))) {
            checkMandatoryHeaders(foundAction, foundTo);
        }

        // wsaw:Anonymous validation only on the server-side
        if (packet.proxy == null) {
            WSDLBoundOperation wbo = getWSDLBoundOperation(packet);
            checkAnonymousSemantics(wbo, replyTo, faultTo);
        }
    }

    final boolean isInCurrentRole(Header header, WSBinding binding) {
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

    final WSDLBoundOperation getWSDLBoundOperation(Packet packet) {
        WSDLBoundOperation wbo = null;
        if (wsdlPort != null) {
            wbo = packet.getMessage().getOperation(wsdlPort);
        }
        return wbo;
    }

    final void checkAnonymousSemantics(WSDLBoundOperation wbo, WSEndpointReference replyTo, WSEndpointReference faultTo) {
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

    protected abstract void validateAction(Packet packet);
    protected abstract void checkMandatoryHeaders(boolean foundAction, boolean foundTo);
}
