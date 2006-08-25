package com.sun.xml.ws.api.addressing;

import org.w3c.dom.Element;

import javax.xml.bind.annotation.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import javax.xml.ws.W3CEndpointReference;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.namespace.QName;
import java.util.Map;
import java.util.List;

/**
 * This class represents a W3C Addressing EndpointReferece which is
 * a remote reference to a web service endpoint that supports the
 * W3C WS-Addressing 1.0 - Core Recommendation.
 * <p>
 * Developers should use this class in their SEIs if they want to
 * pass/return endpoint references that represent the W3C WS-Addressing
 * recommendation.
 * <p>
 * JAXB will use the JAXB annotations and bind this class to XML infoset
 * that is consistent with that defined by WS-Addressing.  See
 * http://www.w3.org/TR/2006/REC-ws-addr-core-20060509/
 * for more information on WS-Addressing EndpointReferences.
 *
 * @since JAX-WS 2.1
 */

// XmlRootElement allows this class to be marshalled on its own
@XmlRootElement(name="EndpointReference",namespace= MemberSubmissionEndpointReference.NS)
@XmlType(name="EndpointReferenceType",namespace= MemberSubmissionEndpointReference.NS)
public final class MemberSubmissionEndpointReference extends EndpointReference {

    private final static JAXBContext msjc = MemberSubmissionEndpointReference.getMSJaxbContext();
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;

    /**
     * construct an EPR from infoset representation
     *
     * @param source A source object containing valid XmlInfoset
     * instance consistent with the W3C WS-Addressing Core
     * recommendation.
     *
     * @throws javax.xml.ws.WebServiceException
     *   if the source does not contain a valid W3C WS-Addressing
     *   EndpointReference.
     * @throws NullPointerException
     *   if the <code>null</code> <code>source</code> value is given
     */
    public MemberSubmissionEndpointReference(Source source) {
        try {
            if (unmarshaller == null)
                unmarshaller = MemberSubmissionEndpointReference.msjc.createUnmarshaller();
            MemberSubmissionEndpointReference epr = (MemberSubmissionEndpointReference)unmarshaller.unmarshal(source);
            this.address = epr.address;
            this.metadata = epr.metadata;
            this.referenceParameters = epr.referenceParameters;
        } catch (JAXBException e) {
            throw new WebServiceException("Error unmarshalling W3CEndpointReference " ,e);
        } catch (ClassCastException e) {
            throw new WebServiceException("Source did not contain W3CEndpointReference", e);
        }
    }

    /**
     * {@inheritDoc}
     **/
    public void writeTo(Result result){
        try {
            if (marshaller == null)
                marshaller = MemberSubmissionEndpointReference.msjc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.marshal(this, result);
        } catch (JAXBException e) {
            throw new WebServiceException("Error marshalling W3CEndpointReference. ", e);
        }
    }

    private final static JAXBContext getMSJaxbContext() {
        try {
            return JAXBContext.newInstance(MemberSubmissionEndpointReference.class);
        } catch (JAXBException e) {
            throw new WebServiceException("Error creating JAXBContext for W3CEndpointReference. ", e);
        }
    }

    // private but necessary properties for databinding
    @XmlElement(name="Address",namespace= MemberSubmissionEndpointReference.NS)
    private MemberSubmissionEndpointReference.Address address;
    @XmlElement(name="ReferenceParameters",namespace= MemberSubmissionEndpointReference.NS)
    private MemberSubmissionEndpointReference.Elements referenceParameters;
    @XmlElement(name="Metadata",namespace= MemberSubmissionEndpointReference.NS)
    private MemberSubmissionEndpointReference.Elements metadata;

    private static class Address {
        @XmlValue
        String uri;
        @XmlAnyAttribute
        Map<QName,String> attributes;
    }


    private static class Elements {
        @XmlAnyElement
        List<Element> elements;
        @XmlAnyAttribute
        Map<QName,String> attributes;
    }

    // Could use MemberSubmissionAdressingConstants - but
    // shouldn't it be defined here for databinding...
    protected static final String NS = "http://schemas.xmlsoap.org/ws/2004/08/addressing";
}
