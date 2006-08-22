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

import java.util.UUID;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.Detail;
import javax.xml.soap.SOAPMessage;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Headers;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.wsdl.WSDLOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.model.wsdl.WSDLFault;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.model.wsdl.WSDLOperationImpl;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;
import com.sun.xml.ws.addressing.model.AddressingProperties;
import com.sun.xml.ws.addressing.model.Elements;
import com.sun.xml.ws.addressing.model.InvalidMapException;
import com.sun.xml.ws.addressing.model.MapRequiredException;
import com.sun.xml.ws.addressing.model.ActionNotSupportedException;
import com.sun.xml.ws.addressing.model.Relationship;
import com.sun.xml.ws.util.xml.XmlUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

/**
 * @author Arun Gupta
 */
public abstract class WsaPipeHelper {

    public final Packet writeClientOutboundHeaders(Packet packet) {
        AddressingProperties ap = new AddressingProperties();

        // wsa:To
        EndpointAddress to = packet.endpointAddress;
        if (to == null)
            to = EndpointAddress.create("http://null.ENDPOINT_ADDRESS_PROPERTY");
        ap.setTo(to.toString());

        // wsa:MessageID
        ap.setMessageID("uuid:" + UUID.randomUUID().toString());

        // wsa:ReplyTo
        // null or "true" is equivalent to request/response MEP
        if (wsdlPort != null && !packet.getMessage().isOneWay(wsdlPort)) {
            EndpointReferenceImpl epr = new EndpointReferenceImpl();
            ap.setReplyTo(epr);
        }

        // wsa:Action
        //
        // Per WS-Addressing spec, these must always be the same. The precedence
        // order to generate this header is:
        //     1. Value of wsaw:Action in WSDL
        //     2. If SOAPAction != null && !SOAPAction.equals(""), then SOAPAction
        //     3. Default value based on (namespace URI)/portTypeName/operation.
        String action = getInputAction(packet);
        if (isInputActionDefault(packet) && (packet.soapAction != null && !packet.soapAction.equals(""))) {
            action = packet.soapAction;
        }
        ap.setAction(action == null ? "http://fake.input.action" : action);
        packet.soapAction = ap.getAction();

        writeHeaders(packet, ap);

        return packet;
    }

    public final void writeHeaders(Packet packet, AddressingProperties ap) {
        Message message = packet.getMessage();
        HeaderList hl = message.getHeaders();
        SOAPVersion soapVersion = binding.getSOAPVersion();

        if (ap.getTo() != null && !ap.getTo().equals("")) {
            hl.add(Headers.create(soapVersion, marshaller, getToQName(), ap.getTo()));
        }

        if (ap.getMessageID() != null && !ap.getMessageID().equals("")) {
            hl.add(Headers.create(soapVersion, marshaller, getMessageIDQName(), ap.getMessageID()));
        }

        if (ap.getFrom() != null && !ap.getFrom().equals("")) {
            hl.add(Headers.create(soapVersion, marshaller, getFromQName(), ap.getFrom()));
        }

        if (ap.getReplyTo() != null && !ap.getReplyTo().equals("")) {
            hl.add(Headers.create(soapVersion, marshaller, getReplyToQName(), ap.getReplyTo()));
        }

        if (ap.getFaultTo() != null && !ap.getFaultTo().equals("")) {
            hl.add(Headers.create(soapVersion, marshaller, getFaultToQName(), ap.getFaultTo()));
        }

        if (ap.getAction() != null && !ap.getAction().equals("")) {
            hl.add(Headers.create(soapVersion, marshaller, getActionQName(), ap.getAction()));
            packet.soapAction = ap.getAction();
        }

        if (ap.getReferenceParameters() != null && !ap.getReferenceParameters().getElements().isEmpty()) {
            Elements refps = ap.getReferenceParameters();
            for (Element refp : refps.getElements()) {
                hl.add(Headers.create(refp));
            }
        }

        writeRelatesTo(ap, hl, soapVersion);
    }


    public final Packet readServerInboundHeaders(Packet packet) {
        // no need to re-read WS-A headers if already read
        if (packet.invocationProperties.get(AddressingConstants.SERVER_INBOUND) != null) {
            return prepareOutbound(packet, false);
        }

        if (wsdlPort != null)
            wbo = packet.getMessage().getOperation(wsdlPort);
        AddressingProperties inbound;

        SOAPFault soapFault;
        Element s11FaultDetail = getSoap11FaultDetail();
        StringBuffer mid = new StringBuffer();

        try {
            inbound = readInboundHeaders(packet, mid);
            packet.invocationProperties.put(AddressingConstants.SERVER_INBOUND, inbound);

            checkMandatoryHeaders(inbound);

            checkAnonymousSemantics(wbo, inbound);

            return prepareOutbound(packet, true);
        } catch (InvalidMapException e) {
            soapFault = newInvalidMapFault(e);
            getInvalidMapDetail(e.getMapQName(), s11FaultDetail);
        } catch (MapRequiredException e) {
            soapFault = newMapRequiredFault(e);
            getMapRequiredDetail(e.getMapQName(), s11FaultDetail);
        } catch (ActionNotSupportedException e) {
            soapFault = newActionNotSupportedFault(e.getAction());
            getProblemActionDetail(e.getAction(), s11FaultDetail);
        }

        if (soapFault != null) {
            Message m = Messages.create(soapFault);
            Header defaultFaultAction = Headers.create(binding.getSOAPVersion(), marshaller, getActionQName(), getDefaultFaultAction());
            Header actionHeader = m.getHeaders().get(getActionQName(), true);
            if (actionHeader == null) {
                m.getHeaders().add(defaultFaultAction);
            } else {
                // TODO: use the default fault actions for service-specific faults
                // TODO: this will make W3C CR happy for now

                // remove any existing Action headers
                m.getHeaders().remove(actionHeader);

                // add the default fault action header
                m.getHeaders().add(defaultFaultAction);
            }

            if (binding.getSOAPVersion() == SOAPVersion.SOAP_11) {
                m.getHeaders().add(Headers.create(s11FaultDetail));
            }

            if (mid != null) {
                Relationship rel = new Relationship(mid.toString(), getRelationshipType());
                m.getHeaders().add(Headers.create(binding.getSOAPVersion(), marshaller, getRelatesToQName(), rel));
            }

            return packet.createResponse(m);
        }

        return packet;
    }

    private AddressingProperties readInboundHeaders(Packet packet, StringBuffer mid) {
        Message message = packet.getMessage();

        if (message == null)
            return null;

        if (message.getHeaders() == null)
            return null;

        AddressingProperties ap = new AddressingProperties();
        java.util.Iterator<Header> hIter = message.getHeaders().getHeaders(getNamespaceURI(), true);

        WSDLPortImpl impl = (WSDLPortImpl)wsdlPort;

        if (wsdlPort != null && impl.isAddressingEnabled() && !impl.isAddressingRequired() && !hIter.hasNext())
            return null;

        try {
            Header midHeader = message.getHeaders().get(getMessageIDQName(), true);
            if (midHeader != null) {
                mid.append(((String)((JAXBElement)midHeader.readAsJAXB(unmarshaller)).getValue()));
            }

            QName faultyHeader = null;

            while (hIter.hasNext()) {
                Header h = hIter.next();

                // check if the Header is in current role
                if (!isInCurrentRole(h)) {
                    continue;
                }

                String local = h.getLocalPart();
                if (local.equals(getFromQName().getLocalPart())) {
                    if (ap.getFrom() != null) {
                        faultyHeader = getFromQName();
                        break;
                    }
                    ap.setFrom((EndpointReference)((JAXBElement)h.readAsJAXB(unmarshaller)).getValue());
                } else if (local.equals(getToQName().getLocalPart())) {
                    if (ap.getTo() != null) {
                        faultyHeader = getToQName();
                        break;
                    }
                    ap.setTo((String)((JAXBElement)h.readAsJAXB(unmarshaller)).getValue());
                } else if (local.equals(getReplyToQName().getLocalPart())) {
                    if (ap.getReplyTo() != null) {
                        faultyHeader = getReplyToQName();
                        break;
                    }
                    ap.setReplyTo((EndpointReference)((JAXBElement)h.readAsJAXB(unmarshaller)).getValue());
                } else if (local.equals(getFaultToQName().getLocalPart())) {
                    if (ap.getFaultTo() != null) {
                        faultyHeader = getFaultToQName();
                        break;
                    }
                    ap.setFaultTo((EndpointReference)((JAXBElement)h.readAsJAXB(unmarshaller)).getValue());
                } else if (local.equals(getActionQName().getLocalPart())) {
                    if (ap.getAction() != null) {
                        faultyHeader = getActionQName();
                        break;
                    }
                    ap.setAction((String)((JAXBElement)h.readAsJAXB(unmarshaller)).getValue());
                } else if (local.equals(getMessageIDQName().getLocalPart())) {
                    if (ap.getMessageID() != null) {
                        faultyHeader = getMessageIDQName();
                        break;
                    }
                    ap.setMessageID((String)((JAXBElement)h.readAsJAXB(unmarshaller)).getValue());
                } else if (local.equals(getRelatesToQName().getLocalPart())) {
                    String mid2 = (String)((JAXBElement)h.readAsJAXB(unmarshaller)).getValue();
                    Relationship rel = newRelationship(mid2);
                    ap.getRelatesTo().add(rel);
                } else if (local.equals(getFaultDetailQName().getLocalPart())) {
                    // TODO: should anything be done here ?
                    // TODO: fault detail element - only for SOAP 1.1
                } else {
                    throw new WebServiceException("unknown WS-A header");
                }
            }

            if (faultyHeader != null) {
                throw new InvalidMapException(faultyHeader, W3CAddressingConstants.INVALID_CARDINALITY);
            }

            HeaderList hl = message.getHeaders();
            for (Header h : hl) {
                if (isReferenceParameter(h)) {
                    ap.getReferenceParameters().getElements().add(unmarshalRefp(h));
                }
            }

        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }

        return ap;
    }

    private boolean isReferenceParameter(Header h) {
        String val = h.getAttribute(getIsReferenceParameterQName());

        return (val != null && Boolean.valueOf(val));
    }

    private boolean isInCurrentRole(Header header) {
        String role;

        // TODO: binding will be null for protocol messages
        // TODO: returning true assumes that protocol messages are
        // TODO: always in current role, this may not to be fixed.
        if (binding == null)
            return true;

        if (binding.getSOAPVersion() == SOAPVersion.SOAP_11) {
            return true;
        } else {
            role = header.getAttribute(s12Role);

            return !(role != null && role.equals(SOAPConstants.URI_SOAP_1_2_ROLE_NONE));
        }
    }

    private void checkMandatoryHeaders(AddressingProperties ap) {
        WSDLPortImpl impl = null;

        if (wsdlPort != null)
            impl = (WSDLPortImpl)wsdlPort;

        if (impl != null && impl.isAddressingRequired() && ap == null) {
            throw new WebServiceException("No WS-A headers are found"); // TODO: i18n
        }

        if (impl != null && impl.isAddressingRequired() && ap.getAction() == null) {
            throw new MapRequiredException(getActionQName());
        }

        if (impl != null && impl.isAddressingRequired() && ap.getTo() == null) {
            throw new MapRequiredException(getToQName());
        }

        // TODO: Add check for other WS-A headers based upon MEP
    }

    private Packet prepareOutbound(Packet packet, boolean validateAction) {
        AddressingProperties inbound = (AddressingProperties)packet.invocationProperties.get(AddressingConstants.SERVER_INBOUND);

        //There may not be a WSDL operation.  There may not even be a WSDL.
        //For instance this may be a RM CreateSequence message.
        if (wsdlPort != null) {
            wbo = packet.getMessage().getOperation(wsdlPort);
        }

        WSDLOperation op = null;

        if (wbo != null) {
            op = wbo.getOperation();
        }

        if (wbo == null || op == null) {
            return packet;
        }

        if (op.isOneWay()) {
            if (validateAction)
                validateAction(packet, inbound.getAction());
            return packet;
        }

        AddressingProperties outbound = toOutbound(inbound, packet);

        if (packet.invocationProperties.get(AddressingConstants.SERVER_OUTBOUND) == null) {
            packet.invocationProperties.put(AddressingConstants.SERVER_OUTBOUND, outbound);
        }

        if (inbound != null && inbound.getAction() != null && validateAction)
            validateAction(packet, inbound.getAction());

        return packet;
    }

    public final AddressingProperties toOutbound(AddressingProperties inbound, Packet packet) {
        AddressingProperties outbound = new AddressingProperties();
        try {
            if (inbound != null)
                outbound = toReply(inbound);
        } catch (MapRequiredException e) {
            if (wsdlPort != null) {
                WSDLPortImpl impl = (WSDLPortImpl)wsdlPort;
                if (impl.isAddressingRequired())
                    throw e;
            }
        }
        outbound.setAction(getOutputAction(packet));

        return outbound;
    }

    public final Packet writeServerOutboundHeaders(Packet packet) {
        // outbound addressing context is populated only for non oneway messages
        AddressingProperties outbound = (AddressingProperties)packet.invocationProperties.get(AddressingConstants.SERVER_OUTBOUND);
        if (outbound == null) {
            return packet;
        }

        Message message = packet.getMessage();
        if (message == null) {
            return packet;
        }

        // set FaultTo reference parameters
        if (message.isFault()) {
            AddressingProperties inbound = (AddressingProperties)packet.invocationProperties.get(AddressingConstants.SERVER_INBOUND);
            outbound = toFault(inbound);

            String action = getFaultAction(message);
            if (action != null)
                outbound.setAction(action);
        }

        if (outbound.getTo() == null) {
            EndpointAddress to = packet.endpointAddress;
            if (to == null)
                outbound.setTo(getAnonymousURI());
            else
                outbound.setTo(to.toString());
        }

        writeHeaders(packet, outbound);

        return packet;
    }

    private String getFaultAction(Message message) {
        String action = getDefaultFaultAction();

        if (wsdlPort == null)
            return null;

        try {
            SOAPMessage sm = message.readAsSOAPMessage();
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

/*
    private AddressingProperties readInboundHeaders(Packet packet) {
        StringBuffer mid = new StringBuffer();
        return readInboundHeaders(packet, mid);
    }

    public final Packet readClientInboundHeaders(Packet packet) {
        AddressingProperties ap = readInboundHeaders(packet);

        if (ap != null) {
            Set<String> set = packet.getHandlerScopePropertyNames(true);
            if (set.contains(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES))
                set.remove(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES);

            packet.invocationProperties.put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, ap);
        }

        return packet;
    }

    private static QName tagname(Message message) {
        SOAPMessage sm;
        try {
            sm = message.readAsSOAPMessage();
            Node detail = sm.getSOAPBody().getFault().getDetail().getFirstChild();
            return new QName(detail.getNamespaceURI(), detail.getLocalName());
        } catch (SOAPException e) {
            throw new AddressingException(e);
        }
    }
*/

    private static Element unmarshalRefp(Header h) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element element = doc.createElement("wrapper");
            DOMResult dom = new DOMResult();
            dom.setNode(element);
            XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(dom);
            h.writeTo(xsw);
            DOMSource source = new DOMSource(dom.getNode().getFirstChild());
            DOMResult result = new DOMResult();

            XmlUtil.newTransformer().transform(source, result);
            return (Element)result.getNode().getFirstChild();
        } catch (Exception ex) {
            throw new WebServiceException(ex);
        }
    }

    private void validateAction(Packet packet, String gotA) {
        // TODO: For now, validation happens only on server-side
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

    private String getInputAction(Packet packet) {
        String action = null;

        if (wsdlPort != null) {
            if (wsdlPort.getBinding() != null) {
                WSDLBoundOperation wbo = wsdlPort.getBinding().getOperation(packet.getMessage().getPayloadNamespaceURI(), packet.getMessage().getPayloadLocalPart());
                if (wbo != null) {
                    WSDLOperation op = wbo.getOperation();
                    if (op != null) {
                        action = ((WSDLOperationImpl)op).getInput().getAction();
                    }
                }
            }
        }

        return action;
    }

    private boolean isInputActionDefault(Packet packet) {
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

    private String getOutputAction(Packet packet) {
        String action = "http://fake.output.action";

        if (wsdlPort != null) {
            if (wsdlPort.getBinding() != null) {
                WSDLBoundOperation wbo = wsdlPort.getBinding().getOperation(packet.getMessage().getPayloadNamespaceURI(), packet.getMessage().getPayloadLocalPart());
                if (wbo != null) {
                    WSDLOperationImpl op = (WSDLOperationImpl)wbo.getOperation();
                    if (op != null) {
                        action = op.getOutput().getAction();
                    }
                }
            }
        }

        return action;
    }

    private SOAPFault newActionNotSupportedFault(String action) {
        QName subcode = getActionNotSupportedQName();
        String faultstring = String.format(getActionNotSupportedText(), action);

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

    private SOAPFault newInvalidMapFault(InvalidMapException e) {
        QName name = e.getMapQName();
        QName subsubcode = e.getSubsubcode();
        QName subcode = getInvalidMapQName();
        String faultstring = String.format(getInvalidMapText(), name, subsubcode);

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

    private SOAPFault newMapRequiredFault(MapRequiredException e) {
        QName subcode = getMapRequiredQName();
        QName subsubcode = getMapRequiredQName();
        String faultstring = getMapRequiredText();

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

    protected void checkAnonymousSemantics(WSDLBoundOperation wbo, AddressingProperties ap) {
    }

    protected abstract void getProblemActionDetail(String action, Element element);
    protected abstract void getInvalidMapDetail(QName name, Element element);
    protected abstract void getMapRequiredDetail(QName name, Element element);
    protected abstract SOAPElement getSoap11FaultDetail();

    protected abstract String getNamespaceURI();
    protected abstract QName getMessageIDQName();
    protected abstract QName getFromQName();
    protected abstract QName getToQName();
    protected abstract QName getReplyToQName();
    protected abstract QName getFaultToQName();
    protected abstract QName getActionQName();
    protected abstract QName getRelatesToQName();
    protected abstract QName getRelationshipTypeQName();
    protected abstract QName getFaultDetailQName();
    protected abstract String getDefaultFaultAction();
    protected abstract QName getIsReferenceParameterQName();
    protected abstract QName getMapRequiredQName();
    protected abstract String getMapRequiredText();
    protected abstract QName getActionNotSupportedQName();
    protected abstract String getActionNotSupportedText();
    protected abstract QName getInvalidMapQName();
    protected abstract String getInvalidMapText();
    protected abstract AddressingProperties toReply(AddressingProperties ap);
    protected abstract AddressingProperties toFault(AddressingProperties ap);
    protected abstract String getAnonymousURI();
    protected abstract String getRelationshipType();
    protected abstract void writeRelatesTo(AddressingProperties ap, HeaderList hl, SOAPVersion soapVersion);
    protected abstract Relationship newRelationship(Relationship r);
    protected abstract Relationship newRelationship(String mid);


    protected static final DocumentBuilder db;

    static {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new WebServiceException(e);
        }
    }


    protected JAXBContext jc;
    protected Unmarshaller unmarshaller;
    protected Marshaller marshaller;

    protected SEIModel seiModel;
    protected WSDLPort wsdlPort;
    protected WSBinding binding;
    private WSDLBoundOperation wbo;

    private static final QName s11Role = new QName(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, "Actor");
    private static final QName s12Role = new QName(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "role");

}
