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

import javax.xml.XMLConstants;
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

import static com.sun.xml.ws.addressing.W3CAddressingConstants.*;
import com.sun.xml.ws.addressing.model.AddressingProperties;
import com.sun.xml.ws.addressing.model.Elements;
import com.sun.xml.ws.addressing.model.InvalidMapException;
import com.sun.xml.ws.addressing.model.MapRequiredException;
import com.sun.xml.ws.addressing.model.Relationship;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Headers;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.model.wsdl.WSDLBoundOperationImpl;
import com.sun.xml.ws.message.RelatesToHeader;
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

    public WsaPipeHelperImpl() {
        try {
            unmarshaller = jc.createUnmarshaller();
            marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }

    public WsaPipeHelperImpl(WSDLPort wsdlPort, WSBinding binding) {
        this();
        this.wsdlPort = wsdlPort;
        this.binding = binding;
    }

    @Override
    public final void getProblemActionDetail(String action, Element element) {
        ProblemAction pa = new ProblemAction(action);
        try {
            marshaller.marshal(pa, element);
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }

    @Override
    public final void getInvalidMapDetail(QName name, Element element) {
        ProblemHeaderQName phq = new ProblemHeaderQName(name);
        try {
            marshaller.marshal(phq, element);
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }

    @Override
    public final void getMapRequiredDetail(QName name, Element element) {
        getInvalidMapDetail(name, element);
    }

    @Override
    public final SOAPElement getSoap11FaultDetail() {
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
                throw new InvalidMapException(getReplyToQName(), ONLY_ANONYMOUS_ADDRESS_SUPPORTED);

            if (faultTo != null && !faultTo.equals(getAnonymousURI()))
                throw new InvalidMapException(getFaultToQName(), ONLY_ANONYMOUS_ADDRESS_SUPPORTED);

        } else if (anon == WSDLBoundOperationImpl.ANONYMOUS.prohibited) {
            if (replyTo != null && replyTo.equals(getAnonymousURI()))
                throw new InvalidMapException(getReplyToQName(), ONLY_NON_ANONYMOUS_ADDRESS_SUPPORTED);

            if (faultTo != null && faultTo.equals(getAnonymousURI()))
                throw new InvalidMapException(getFaultToQName(), ONLY_NON_ANONYMOUS_ADDRESS_SUPPORTED);

        } else {
            // cannot reach here
            throw new WebServiceException("Invalid value in TWsaWSDLBindingOperationExtension: \"" + anon + "\"");
        }
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
    public String getNamespaceURI() {
        return WSA_NAMESPACE_NAME;
    }

    @Override
    public QName getMessageIDQName() {
        return WSA_MESSAGEID_QNAME;
    }

    @Override
    public QName getFromQName() {
        return WSA_FROM_QNAME;
    }

    @Override
    public QName getToQName() {
        return WSA_TO_QNAME;
    }

    @Override
    public QName getReplyToQName() {
        return WSA_REPLYTO_QNAME;
    }

    @Override
    public QName getFaultToQName() {
        return WSA_FAULTTO_QNAME;
    }

    @Override
    public QName getActionQName() {
        return WSA_ACTION_QNAME;
    }

    @Override
    public QName getRelatesToQName() {
        return WSA_RELATESTO_QNAME;
    }

    @Override
    public QName getRelationshipTypeQName() {
        return WSA_RELATIONSHIPTYPE_QNAME;
    }

    @Override
    public String getDefaultFaultAction() {
        return WSA_DEFAULT_FAULT_ACTION;
    }

    @Override
    public QName getIsReferenceParameterQName() {
        return WSA_REFERENCEPARAMETERS_QNAME;
    }

    @Override
    public QName getMapRequiredQName() {
        return MAP_REQUIRED_QNAME;
    }

    @Override
    public String getMapRequiredText() {
        return MAP_REQUIRED_TEXT;
    }

    @Override
    public QName getActionNotSupportedQName() {
        return ACTION_NOT_SUPPORTED_QNAME;
    }

    @Override
    public String getActionNotSupportedText() {
        return ACTION_NOT_SUPPORTED_TEXT;
    }

    @Override
    public QName getFaultDetailQName() {
        return FAULT_DETAIL_QNAME;
    }

    @Override
    public QName getInvalidMapQName() {
        return INVALID_MAP_QNAME;
    }

    @Override
    public String getInvalidMapText() {
        return INVALID_MAP_TEXT;
    }

    @Override
    public AddressingProperties toReply(AddressingProperties ap) {
        return toReplyOrFault(ap, false);
    }

    @Override
    public AddressingProperties toFault(AddressingProperties ap) {
        return toReplyOrFault(ap, true);
    }

    @Override
    public String getAnonymousURI() {
        return WSA_ANONYMOUS_ADDRESS;
    }

    @Override
    public String getRelationshipType() {
        return WSA_RELATIONSHIP_REPLY;
    }

    @Override
    public void writeRelatesTo(AddressingProperties ap, HeaderList hl, SOAPVersion soapVersion) {
        if (ap.getRelatesTo() != null && ap.getRelatesTo().size() > 0) {
            for (Relationship rel : ap.getRelatesTo()) {
                hl.add(new RelatesToHeader(getRelatesToQName(), rel.getId(), rel.getType()));
            }
        }
    }

    @Override
    public void writeRelatesTo(Relationship rel, HeaderList hl, SOAPVersion soapVersion) {
        RelationshipImpl reli = (RelationshipImpl)rel;
        hl.add(Headers.create(soapVersion, marshaller, getRelatesToQName(), reli));
    }

    public Relationship newRelationship(Relationship r) {
        return new RelationshipImpl(r.getId(), r.getType());
    }

    public Relationship newRelationship(String mid) {
        return new RelationshipImpl(mid);
    }

    public EndpointReference newEndpointReference() {
        return new EndpointReferenceImpl();
    }

    public QName getInvalidCardinalityQName() {
        return W3CAddressingConstants.INVALID_CARDINALITY;
    }

    public String getNoneURI() {
        return W3CAddressingConstants.WSA_NONE_ADDRESS;
    }

    public String getAddress(EndpointReference epr) {
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
            throw new InvalidMapException(INVALID_MAP_QNAME, MISSING_ADDRESS_IN_EPR);

        props.setTo(uri);

        Elements params = ((EndpointReferenceImpl)source).getRefParams();

        if (params != null) {
            for (Element refp : params.getElements()) {
                addIsRefp(refp);
                props.getReferenceParameters().getElements().add(refp);
            }
        }

        return props;
    }

    void addIsRefp(Element refp) {
        refp.setAttributeNS(WSA_NAMESPACE_NAME,
            WSA_NAMESPACE_PREFIX + ":" + WSA_IS_REFERENCE_PARAMETER_QNAME.getLocalPart(),
            "true");

        // TODO: This may cause a namespace prefix conflict and is a tricky problem
        // TODO: to solve. For example, parent might have a similar prefix used
        // TODO: for some other purpose, etc.
        refp.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,"xmlns:"+WSA_NAMESPACE_PREFIX,
            WSA_NAMESPACE_NAME);
    }
}
