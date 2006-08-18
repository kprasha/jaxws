/*
 The contents of this file are subject to the terms
 of the Common Development and Distribution License
 (the "License").  You may not use this file except
 in compliance with the License.
 
 You can obtain a copy of the license at
 https://jwsdp.dev.java.net/CDDLv1.0.html
 See the License for the specific language governing
 permissions and limitations under the License.
 
 When distributing Covered Code, include this CDDL
 HEADER in each file and include the License file at
 https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 add the following below this CDDL HEADER, with the
 fields enclosed by brackets "[]" replaced with your
 own identifying information: Portions Copyright [yyyy]
 [name of copyright owner]
*/
/*
 $Id: AddressingWsaPipeHelperImpl.java,v 1.1.2.1 2006-08-18 21:56:11 arungupta Exp $

 Copyright (c) 2006 Sun Microsystems, Inc.
 All rights reserved.
*/

package com.sun.xml.ws.addressing;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.model.wsdl.WSDLBoundOperationImpl;
import org.w3c.dom.Element;

/**
 * @author Arun Gupta
 */
public class AddressingWsaPipeHelperImpl extends WsaPipeHelper {

    private AddressingWsaPipeHelperImpl() {
        try {
            jc = JAXBContext.newInstance(EndpointReferenceImpl.class,
                                         ObjectFactory.class);
            unmarshaller = jc.createUnmarshaller();
            marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }

    public AddressingWsaPipeHelperImpl(SEIModel seiModel, WSDLPort wsdlPort, WSBinding binding) {
        this();
        this.seiModel = seiModel;
        this.wsdlPort = wsdlPort;
        this.binding = binding;
    }

    @Override
    protected final void getProblemActionDetail(String action, Element element) {
        ProblemAction pa = new ProblemAction(action);
        try {
            marshaller.marshal(pa, element);
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }

    @Override
    protected final void getInvalidMapDetail(QName name, Element element) {
        ProblemHeaderQName phq = new ProblemHeaderQName(name);
        try {
            marshaller.marshal(phq, element);
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }

    @Override
    protected final void getMapRequiredDetail(QName name, Element element) {
        getInvalidMapDetail(name, element);
    }

    @Override
    protected final SOAPElement getSoap11FaultDetail() {
        try {
            if (binding == null)
                return null;

            return binding.getSOAPVersion().saajSoapFactory.createElement(new QName(W3CAddressingConstants.WSA_NAMESPACE_NAME, "FaultDetail"));
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }

    @Override
    protected final void checkAnonymousSemantics(WSDLBoundOperation wbo, AddressingProperties ap) {
        if (wbo == null)
            return;

        if (ap == null)
            return;

        WSDLBoundOperationImpl impl = (WSDLBoundOperationImpl)wbo;
        WSDLBoundOperationImpl.ANONYMOUS anon = impl.getAnonymous();

        String replyTo = null;
        String faultTo = null;

        if (ap.getReplyTo() != null)
            replyTo = ((EndpointReferenceImpl) ap.getReplyTo()).getAddress();

        if (ap.getFaultTo() != null)
            faultTo = ((EndpointReferenceImpl) ap.getFaultTo()).getAddress();

        if (anon == WSDLBoundOperationImpl.ANONYMOUS.optional) {
            // no check is required
        } else if (anon == WSDLBoundOperationImpl.ANONYMOUS.required) {
            if (replyTo != null && !replyTo.equals(getAnonymousURI()))
                throw new InvalidMapException(getReplyToQName(), W3CAddressingConstants.ONLY_ANONYMOUS_ADDRESS_SUPPORTED);

            if (faultTo != null && !faultTo.equals(getAnonymousURI()))
                throw new InvalidMapException(getFaultToQName(), W3CAddressingConstants.ONLY_ANONYMOUS_ADDRESS_SUPPORTED);

        } else if (anon == WSDLBoundOperationImpl.ANONYMOUS.prohibited) {
            if (replyTo != null && replyTo.equals(getAnonymousURI()))
                throw new InvalidMapException(getReplyToQName(), W3CAddressingConstants.ONLY_NON_ANONYMOUS_ADDRESS_SUPPORTED);

            if (faultTo != null && faultTo.equals(getAnonymousURI()))
                throw new InvalidMapException(getFaultToQName(), W3CAddressingConstants.ONLY_NON_ANONYMOUS_ADDRESS_SUPPORTED);

        } else {
            // cannot reach here
            throw new WebServiceException("Invalid value in TWsaWSDLBindingOperationExtension: \"" + anon + "\"");
        }
    }

    @XmlRegistry
    final class ObjectFactory {
        @XmlElementDecl(namespace=W3CAddressingConstants.WSA_NAMESPACE_NAME,name="From")
        final JAXBElement<EndpointReferenceImpl> createFrom(EndpointReferenceImpl u) {
            return null;
        }

        @XmlElementDecl(namespace=W3CAddressingConstants.WSA_NAMESPACE_NAME,name="Action")
        final JAXBElement<String> createAction(String u) {
            return null;
        }

        @XmlElementDecl(namespace=W3CAddressingConstants.WSA_NAMESPACE_NAME,name="To")
        final JAXBElement<String> createTo(String u) {
            return null;
        }

        @XmlElementDecl(namespace=W3CAddressingConstants.WSA_NAMESPACE_NAME,name="ReplyTo")
        final JAXBElement<EndpointReferenceImpl> createReplyTo(EndpointReferenceImpl u) {
            return null;
        }

        @XmlElementDecl(namespace=W3CAddressingConstants.WSA_NAMESPACE_NAME,name="FaultTo")
        final JAXBElement<EndpointReferenceImpl> createFaultTo(EndpointReferenceImpl u) {
            return null;
        }

        @XmlElementDecl(namespace=W3CAddressingConstants.WSA_NAMESPACE_NAME,name="MessageID")
        final JAXBElement<String> createMessageID(String u) {
            return null;
        }

        @XmlElementDecl(namespace=W3CAddressingConstants.WSA_NAMESPACE_NAME,name="RelatesTo")
        final JAXBElement<String> createRelationship(String u) {
            return null;
        }

    }

    @Override
    protected String getNamespaceURI() {
        return W3CAddressingConstants.WSA_NAMESPACE_NAME;
    }

    @Override
    protected QName getMessageIDQName() {
        return W3CAddressingConstants.WSA_MESSAGEID_QNAME;
    }

    @Override
    protected QName getFromQName() {
        return W3CAddressingConstants.WSA_FROM_QNAME;
    }

    @Override
    protected QName getToQName() {
        return W3CAddressingConstants.WSA_TO_QNAME;
    }

    @Override
    protected QName getReplyToQName() {
        return W3CAddressingConstants.WSA_REPLYTO_QNAME;
    }

    @Override
    protected QName getFaultToQName() {
        return W3CAddressingConstants.WSA_FAULTTO_QNAME;
    }

    @Override
    protected QName getActionQName() {
        return W3CAddressingConstants.WSA_ACTION_QNAME;
    }

    @Override
    protected QName getRelatesToQName() {
        return W3CAddressingConstants.WSA_RELATESTO_QNAME;
    }

    @Override
    protected QName getRelationshipTypeQName() {
        return W3CAddressingConstants.WSA_RELATIONSHIPTYPE_QNAME;
    }

    @Override
    protected String getDefaultFaultAction() {
        return W3CAddressingConstants.WSA_DEFAULT_FAULT_ACTION;
    }

    @Override
    protected QName getIsReferenceParameterQName() {
        return W3CAddressingConstants.WSA_REFERENCEPARAMETERS_QNAME;
    }

    @Override
    protected QName getMapRequiredQName() {
        return W3CAddressingConstants.MAP_REQUIRED_QNAME;
    }

    @Override
    protected String getMapRequiredText() {
        return W3CAddressingConstants.MAP_REQUIRED_TEXT;
    }

    @Override
    protected QName getActionNotSupportedQName() {
        return W3CAddressingConstants.ACTION_NOT_SUPPORTED_QNAME;
    }

    @Override
    protected String getActionNotSupportedText() {
        return W3CAddressingConstants.ACTION_NOT_SUPPORTED_TEXT;
    }

    @Override
    protected QName getFaultDetailQName() {
        return W3CAddressingConstants.FAULT_DETAIL_QNAME;
    }

    @Override
    protected QName getInvalidMapQName() {
        return W3CAddressingConstants.INVALID_MAP_QNAME;
    }

    @Override
    protected String getInvalidMapText() {
        return W3CAddressingConstants.INVALID_MAP_TEXT;
    }

    protected AddressingProperties toReply(AddressingProperties ap) {
        return toReplyOrFault(ap, false);
    }

    protected AddressingProperties toFault(AddressingProperties ap) {
        return toReplyOrFault(ap, true);
    }

    protected String getAnonymousURI() {
        return W3CAddressingConstants.WSA_ANONYMOUS_ADDRESS;
    }

    private AddressingProperties toReplyOrFault(AddressingProperties source, boolean isFault) {
        if (source == null) {
            throw new WebServiceException("Source addressing properties is null."); // TODO i18n
        }

        EndpointReference destination;
        if (isFault) {
            destination = (source.getFaultTo() != null) ?
                source.getFaultTo() :
                source.getReplyTo();
        } else {
            destination = source.getReplyTo();
        }

        if (destination == null) {
            destination = new EndpointReferenceImpl();
        }

        AddressingProperties response = toDestination(destination);

        String uri = source.getMessageID();
        if (uri == null) {
            throw new MapRequiredException(W3CAddressingConstants.WSA_MESSAGEID_QNAME);
        }

//        RelationshipImpl impl = new RelationshipImpl(uri.getURI());
//        Map<QName, String> atts = uri.getAttributes();
//        if (atts != null) {
//            for (QName name: atts.keySet()) {
//                impl.addAttribute(name, atts.get(name));
//            }
//        }
//
//        Relationship[] r = new Relationship[1];
//        r[0] = impl;
//        setRelatesTo(r);

        return response;
    }

    private AddressingProperties toDestination(EndpointReference source) {
        AddressingProperties props = new AddressingProperties();

        if (source == null) {
            throw new WebServiceException("Source addressing properties is null."); // TODO i18n
        }

        String uri = ((EndpointReferenceImpl)source).getAddress();
        if (uri == null)
            throw new InvalidMapException(W3CAddressingConstants.INVALID_MAP_QNAME, W3CAddressingConstants.MISSING_ADDRESS_IN_EPR);

        props.setTo(uri);

//        ReferenceParameters params = source.getReferenceParameters();
//
//        if (params != null) {
//            setReferenceParameters(params);
//            for (Object refp : refParams.getElements()) {
//                if (refp instanceof Element)
//                    addIsRefp((Element)refp);
//            }
//        }

        return props;
    }
}
