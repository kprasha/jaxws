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

import java.util.Map;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.WebServiceException;

import com.sun.xml.ws.addressing.model.ActionNotSupportedException;
import com.sun.xml.ws.addressing.model.InvalidMapException;
import com.sun.xml.ws.addressing.model.MapRequiredException;
import static com.sun.xml.ws.addressing.W3CAddressingConstants.ONLY_ANONYMOUS_ADDRESS_SUPPORTED;
import static com.sun.xml.ws.addressing.W3CAddressingConstants.ONLY_NON_ANONYMOUS_ADDRESS_SUPPORTED;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLFault;
import com.sun.xml.ws.api.model.wsdl.WSDLOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.message.FaultDetailHeader;
import com.sun.xml.ws.model.wsdl.WSDLOperationImpl;
import com.sun.xml.ws.model.wsdl.WSDLBoundOperationImpl;
import com.sun.xml.ws.resources.AddressingMessages;
import org.w3c.dom.Element;

/**
 * @author Arun Gupta
 */
public abstract class WsaPipeHelper {

    public final Packet validateServerInboundHeaders(Packet packet) throws XMLStreamException {
        SOAPFault soapFault;
        FaultDetailHeader s11FaultDetailHeader;

        try {
            HeaderValidator.checkCardinality(packet, binding, wsdlPort);

            validateAction(packet);

            return packet;
        } catch (InvalidMapException e) {
            soapFault = newInvalidMapFault(e, binding.getAddressingVersion());
            s11FaultDetailHeader = new FaultDetailHeader(binding.getAddressingVersion(), binding.getAddressingVersion().problemHeaderQNameTag.getLocalPart(), e.getMapQName());
        } catch (MapRequiredException e) {
            soapFault = newMapRequiredFault(e, binding.getAddressingVersion());
            s11FaultDetailHeader = new FaultDetailHeader(binding.getAddressingVersion(), binding.getAddressingVersion().problemHeaderQNameTag.getLocalPart(), e.getMapQName());
        } catch (ActionNotSupportedException e) {
            soapFault = newActionNotSupportedFault(e.getAction(), binding.getAddressingVersion());
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

    private void validateAction(Packet packet) {
        //There may not be a WSDL operation.  There may not even be a WSDL.
        //For instance this may be a RM CreateSequence message.
        WSDLBoundOperation wbo = HeaderValidator.getWSDLBoundOperation(wsdlPort, packet);

        WSDLOperation op = null;

        if (wbo != null) {
            op = wbo.getOperation();
        }

        if (wbo == null || op == null) {
            return;
        }

        String gotA = packet.getMessage().getHeaders().getAction(binding.getAddressingVersion(), binding.getSOAPVersion());

        // For now, validation happens only on server-side.
        // todo: should it happen on client side as well ?
        if (packet.proxy != null) {
            return;
        }

        if (gotA == null)
            throw new WebServiceException("null input action"); // TODO: i18n

        String expected = getInputAction(packet);
        String soapAction = getSOAPAction(packet);
        if (isInputActionDefault(packet) && (soapAction != null && !soapAction.equals("")))
            expected = soapAction;

        if (expected != null && !gotA.equals(expected)) {
            throw new ActionNotSupportedException(gotA);
        }
    }

    private String getFaultAction(Packet packet) {
        String action = binding.getAddressingVersion().getDefaultFaultAction();

        if (wsdlPort == null)
            return null;

        try {
            SOAPMessage sm = packet.getMessage().readAsSOAPMessage();
            if (sm == null)
                return action;

            if (sm.getSOAPBody() == null)
                return action;

            if (sm.getSOAPBody().getFault() == null)
                return action;

            Detail detail = sm.getSOAPBody().getFault().getDetail();
            if (detail == null)
                return action;

            String ns = detail.getFirstChild().getNamespaceURI();
            String name = detail.getFirstChild().getLocalName();

            WSDLBoundOperation wbo = null;
            if (wsdlPort != null)
                wbo = packet.getMessage().getOperation(wsdlPort);

            WSDLOperation o = wbo.getOperation();
            if (o == null)
                return action;

            WSDLFault fault = o.getFault(new QName(ns, name));
            if (fault == null)
                return action;

            WSDLOperationImpl impl = (WSDLOperationImpl)o;
            Map<String,String> map = impl.getFaultActionMap();
            if (map == null)
                return action;

            action = map.get(fault.getName());

            return action;
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }

    public String getInputAction(Packet packet) {
        String action = null;

        if (wsdlPort != null) {
            if (wsdlPort.getBinding() != null) {
                WSDLBoundOperation wbo = wsdlPort.getBinding().getOperation(packet.getMessage().getPayloadNamespaceURI(), packet.getMessage().getPayloadLocalPart());
                if (wbo != null) {
                    WSDLOperation op = wbo.getOperation();
                    if (op != null) {
                        action = op.getInput().getAction();
                    }
                }
            }
        }

        return action;
    }

    public boolean isInputActionDefault(Packet packet) {
        if (wsdlPort == null)
            return false;

        if (wsdlPort.getBinding() == null)
            return false;

        WSDLBoundOperation wbo = wsdlPort.getBinding().getOperation(packet.getMessage().getPayloadNamespaceURI(), packet.getMessage().getPayloadLocalPart());
        if (wbo == null)
            return false;

        WSDLOperation op = wbo.getOperation();
        if (op == null)
            return false;

        return ((WSDLOperationImpl)op).getInput().isDefaultAction();
    }

    private String getSOAPAction(Packet packet) {
        String action = "";

        if (packet == null)
            return action;

        if (packet.getMessage() == null)
            return action;

        WSDLBoundOperation op = packet.getMessage().getOperation(wsdlPort);
        if (op == null)
            return action;

        action = op.getSOAPAction();

        return action;
    }

    public String getOutputAction(Packet packet) {
        String action = AddressingVersion.UNSET_OUTPUT_ACTION;

        if (wsdlPort != null) {
            if (wsdlPort.getBinding() != null) {
                WSDLBoundOperation wbo = wsdlPort.getBinding().getOperation(packet.getMessage().getPayloadNamespaceURI(), packet.getMessage().getPayloadLocalPart());
                if (wbo != null) {
                    WSDLOperation op = wbo.getOperation();
                    if (op != null) {
                        action = op.getOutput().getAction();
                    }
                }
            }
        }

        return action;
    }

    private SOAPFault newActionNotSupportedFault(String action, AddressingVersion av) {
        QName subcode = av.actionNotSupportedTag;
        String faultstring = String.format(av.actionNotSupportedText, action);

        try {
            SOAPFactory factory;
            SOAPFault fault;
            if (binding.getSOAPVersion() == SOAPVersion.SOAP_12) {
                factory = SOAPVersion.SOAP_12.saajSoapFactory;
                fault = factory.createFault();
                fault.setFaultCode(SOAPConstants.SOAP_SENDER_FAULT);
                fault.appendFaultSubcode(subcode);
                getProblemActionDetail(action, fault.addDetail());
            } else {
                factory = SOAPVersion.SOAP_11.saajSoapFactory;
                fault = factory.createFault();
                fault.setFaultCode(subcode);
            }

            fault.setFaultString(faultstring);

            return fault;
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }

    private SOAPFault newInvalidMapFault(InvalidMapException e, AddressingVersion av) {
        QName name = e.getMapQName();
        QName subsubcode = e.getSubsubcode();
        QName subcode = av.invalidMapTag;
        String faultstring = String.format(av.getInvalidMapText(), name, subsubcode);

        try {
            SOAPFactory factory;
            SOAPFault fault;
            if (binding.getSOAPVersion() == SOAPVersion.SOAP_12) {
                factory = SOAPVersion.SOAP_12.saajSoapFactory;
                fault = factory.createFault();
                fault.setFaultCode(SOAPConstants.SOAP_SENDER_FAULT);
                fault.appendFaultSubcode(subcode);
                fault.appendFaultSubcode(subsubcode);
                getInvalidMapDetail(name, fault.addDetail());
            } else {
                factory = SOAPVersion.SOAP_11.saajSoapFactory;
                fault = factory.createFault();
                fault.setFaultCode(subsubcode);
            }

            fault.setFaultString(faultstring);

            return fault;
        } catch (SOAPException se) {
            throw new WebServiceException(se);
        }
    }

    private SOAPFault newMapRequiredFault(MapRequiredException e, AddressingVersion av) {
        QName subcode = av.mapRequiredTag;
        QName subsubcode = av.mapRequiredTag;
        String faultstring = av.getMapRequiredText();

        try {
            SOAPFactory factory;
            SOAPFault fault;
            if (binding.getSOAPVersion() == SOAPVersion.SOAP_12) {
                factory = SOAPVersion.SOAP_12.saajSoapFactory;
                fault = factory.createFault();
                fault.setFaultCode(SOAPConstants.SOAP_SENDER_FAULT);
                fault.appendFaultSubcode(subcode);
                fault.appendFaultSubcode(subsubcode);
                getMapRequiredDetail(e.getMapQName(), fault.addDetail());
            } else {
                factory = SOAPVersion.SOAP_11.saajSoapFactory;
                fault = factory.createFault();
                fault.setFaultCode(subsubcode);
            }

            fault.setFaultString(faultstring);

            return fault;
        } catch (SOAPException se) {
            throw new WebServiceException(se);
        }
    }

    public abstract void getProblemActionDetail(String action, Element element);
    public abstract void getInvalidMapDetail(QName name, Element element);
    public abstract void getMapRequiredDetail(QName name, Element element);

    protected Unmarshaller unmarshaller;
    protected Marshaller marshaller;

    protected WSDLPort wsdlPort;
    protected WSBinding binding;
}
