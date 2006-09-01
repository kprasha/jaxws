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

package com.sun.xml.ws.addressing.v200408;

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

import com.sun.xml.ws.addressing.WsaPipeHelper;
import com.sun.xml.ws.addressing.model.AddressingProperties;
import com.sun.xml.ws.addressing.model.Elements;
import com.sun.xml.ws.addressing.model.InvalidMapException;
import com.sun.xml.ws.addressing.model.MapRequiredException;
import com.sun.xml.ws.addressing.model.Relationship;
import static com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants.*;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Headers;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import org.w3c.dom.Element;

/**
 * @author Arun Gupta
 */
public class WsaPipeHelperImpl extends WsaPipeHelper {
    static final JAXBContext jc;

    static {
        try {
            jc = JAXBContext.newInstance(EndpointReferenceImpl.class,
                                         ObjectFactory.class,
                                         RelationshipImpl.class,
                                         ProblemAction.class,
                                         ProblemHeaderQName.class);
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }


    private WsaPipeHelperImpl() {
        try {
            unmarshaller = jc.createUnmarshaller();
            marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }

    public WsaPipeHelperImpl(SEIModel seiModel, WSDLPort wsdlPort, WSBinding binding) {
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

            return binding.getSOAPVersion().saajSoapFactory.createElement(new QName(WSA_NAMESPACE_NAME, "FaultDetail"));
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }

    @Override
    protected final void checkAnonymousSemantics(WSDLBoundOperation wbo, AddressingProperties ap) {
    }

    @XmlRegistry
    final class ObjectFactory {
        @XmlElementDecl(namespace=WSA_NAMESPACE_NAME,name="From")
        final JAXBElement<EndpointReferenceImpl> createFrom(EndpointReferenceImpl u) {
            return null;
        }

        @XmlElementDecl(namespace=WSA_NAMESPACE_NAME,name="Action")
        final JAXBElement<String> createAction(String u) {
            return null;
        }

        @XmlElementDecl(namespace=WSA_NAMESPACE_NAME,name="To")
        final JAXBElement<String> createTo(String u) {
            return null;
        }

        @XmlElementDecl(namespace=WSA_NAMESPACE_NAME,name="ReplyTo")
        final JAXBElement<EndpointReferenceImpl> createReplyTo(EndpointReferenceImpl u) {
            return null;
        }

        @XmlElementDecl(namespace=WSA_NAMESPACE_NAME,name="FaultTo")
        final JAXBElement<EndpointReferenceImpl> createFaultTo(EndpointReferenceImpl u) {
            return null;
        }

        @XmlElementDecl(namespace=WSA_NAMESPACE_NAME,name="MessageID")
        final JAXBElement<String> createMessageID(String u) {
            return null;
        }

        @XmlElementDecl(namespace=WSA_NAMESPACE_NAME,name="RelatesTo")
        final JAXBElement<RelationshipImpl> createRelationship(RelationshipImpl u) {
            return null;
        }

    }

    @Override
    protected String getNamespaceURI() {
        return WSA_NAMESPACE_NAME;
    }

    @Override
    protected QName getMessageIDQName() {
        return WSA_MESSAGEID_QNAME;
    }

    @Override
    protected QName getFromQName() {
        return WSA_FROM_QNAME;
    }

    @Override
    protected QName getToQName() {
        return WSA_TO_QNAME;
    }

    @Override
    protected QName getReplyToQName() {
        return WSA_REPLYTO_QNAME;
    }

    @Override
    protected QName getFaultToQName() {
        return WSA_FAULTTO_QNAME;
    }

    @Override
    protected QName getActionQName() {
        return WSA_ACTION_QNAME;
    }

    @Override
    protected QName getRelatesToQName() {
        return WSA_RELATESTO_QNAME;
    }

    @Override
    protected QName getRelationshipTypeQName() {
        return WSA_RELATIONSHIPTYPE_QNAME;
    }

    @Override
    protected String getDefaultFaultAction() {
        return WSA_DEFAULT_FAULT_ACTION;
    }

    @Override
    protected QName getIsReferenceParameterQName() {
        return WSA_REFERENCEPARAMETERS_QNAME;
    }

    @Override
    protected QName getMapRequiredQName() {
        return MAP_REQUIRED_QNAME;
    }

    @Override
    protected String getMapRequiredText() {
        return MAP_REQUIRED_TEXT;
    }

    @Override
    protected QName getActionNotSupportedQName() {
        return ACTION_NOT_SUPPORTED_QNAME;
    }

    @Override
    protected String getActionNotSupportedText() {
        return ACTION_NOT_SUPPORTED_TEXT;
    }

    @Override
    protected QName getFaultDetailQName() {
        return FAULT_DETAIL_QNAME;
    }

    @Override
    protected QName getInvalidMapQName() {
        return INVALID_MAP_QNAME;
    }

    @Override
    protected String getInvalidMapText() {
        return INVALID_MAP_TEXT;
    }

    @Override
    protected AddressingProperties toReply(AddressingProperties ap) {
        return toReplyOrFault(ap, false);
    }

    @Override
    protected AddressingProperties toFault(AddressingProperties ap) {
        return toReplyOrFault(ap, true);
    }

    @Override
    protected String getAnonymousURI() {
        return WSA_ANONYMOUS_ADDRESS;
    }

    @Override
    protected String getRelationshipType() {
        return WSA_RELATIONSHIP_REPLY;
    }

    @Override
    protected void writeRelatesTo(AddressingProperties ap, HeaderList hl, SOAPVersion soapVersion) {
        if (ap.getRelatesTo() != null && ap.getRelatesTo().size() > 0) {
            for (Relationship rel : ap.getRelatesTo()) {
                RelationshipImpl reli = (RelationshipImpl)rel;
                hl.add(Headers.create(soapVersion, marshaller, getRelatesToQName(), reli));
            }
        }
    }

    @Override
    protected void writeRelatesTo(Relationship rel, HeaderList hl, SOAPVersion soapVersion) {
        RelationshipImpl reli = (RelationshipImpl)rel;
        hl.add(Headers.create(soapVersion, marshaller, getRelatesToQName(), reli));
    }

    @Override
    protected Relationship newRelationship(Relationship r) {
        return new RelationshipImpl(r.getId(), r.getType());
    }

    protected Relationship newRelationship(String mid) {
        return new RelationshipImpl(mid);
    }

    protected EndpointReference newEndpointReference() {
        return new EndpointReferenceImpl();
    }

    protected QName getInvalidCardinalityQName() {
        return MemberSubmissionAddressingConstants.INVALID_MAP_QNAME;
    }

    protected String getNoneURI() {
        return MemberSubmissionAddressingConstants.WSA_NONE_ADDRESS;
    }

    protected String getAddress(EndpointReference epr) {
        if (epr == null)
            return null;
        else
            return ((EndpointReferenceImpl)epr).getAddress();
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
            throw new MapRequiredException(WSA_MESSAGEID_QNAME);
        }

        response.getRelatesTo().add(new RelationshipImpl(uri, WSA_RELATIONSHIP_REPLY));

        return response;
    }

    private AddressingProperties toDestination(EndpointReference source) {
        AddressingProperties props = new AddressingProperties();

        if (source == null) {
            throw new WebServiceException("Source addressing properties is null."); // TODO i18n
        }

        String uri = ((EndpointReferenceImpl)source).getAddress();
        if (uri == null)
            throw new InvalidMapException(INVALID_MAP_QNAME, WSA_ADDRESS_QNAME);

        props.setTo(uri);

        Elements params = ((EndpointReferenceImpl)source).getRefParams();
        if (params != null) {
            for (Element refp : params.getElements()) {
                props.getReferenceParameters().getElements().add(refp);
            }
        }

        params = ((EndpointReferenceImpl)source).getMetadata();
        if (params != null) {
            for (Element refp : params.getElements()) {
                props.getReferenceParameters().getElements().add(refp);
            }
        }

        return props;
    }
}
