package com.sun.xml.ws.api.addressing;

import com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import java.util.List;
import java.util.Map;

/**
 * This class represents a W3C Addressing EndpointReferece which is
 * a remote reference to a web service endpoint that supports the
 * W3C WS-Addressing 1.0 - Core Recommendation.
 * <p/>
 * Developers should use this class in their SEIs if they want to
 * pass/return endpoint references that represent the W3C WS-Addressing
 * recommendation.
 * <p/>
 * JAXB will use the JAXB annotations and bind this class to XML infoset
 * that is consistent with that defined by WS-Addressing.  See
 * http://www.w3.org/TR/2006/REC-ws-addr-core-20060509/
 * for more information on WS-Addressing EndpointReferences.
 *
 * @since JAX-WS 2.1
 */

// XmlRootElement allows this class to be marshalled on its own
@XmlRootElement(name = "EndpointReference", namespace = MemberSubmissionEndpointReference.MSNS)
@XmlType(name = "MemberSubmissionEndpointReferenceType", namespace = MemberSubmissionEndpointReference.MSNS)
public class MemberSubmissionEndpointReference extends EndpointReference implements MemberSubmissionAddressingConstants {

    private final static JAXBContext msjc = MemberSubmissionEndpointReference.getMSJaxbContext();
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;

    /**
     * Default Constuctor for MemberSubmissionEPR
     */
    //may need public default constructor - kw
    public MemberSubmissionEndpointReference() {
    }

    /**
     * construct an EPR from infoset representation
     *
     * @param source A source object containing valid XmlInfoset
     *               instance consistent with the W3C WS-Addressing Core
     *               recommendation.
     * @throws javax.xml.ws.WebServiceException
     *                              if the source does not contain a valid W3C WS-Addressing
     *                              EndpointReference.
     * @throws NullPointerException if the <code>null</code> <code>source</code> value is given
     */
    public MemberSubmissionEndpointReference(Source source) {

        if (source == null)
            throw new WebServiceException("Source parameter can not be null on constructor");

        try {
            if (unmarshaller == null)
                unmarshaller = MemberSubmissionEndpointReference.msjc.createUnmarshaller();
            MemberSubmissionEndpointReference epr = (MemberSubmissionEndpointReference) unmarshaller.unmarshal(source);
            this.addr = epr.addr;
            this.metadata = epr.metadata;
            this.referenceParameters = epr.referenceParameters;
        } catch (JAXBException e) {
            throw new WebServiceException("Error unmarshalling MemberSubmissionEndpointReference ", e);
        } catch (ClassCastException e) {
            throw new WebServiceException("Source did not contain MemberSubmissionEndpointReference", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeTo(Result result) {
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
            throw new WebServiceException("Error creating JAXBContext for MemberSubmissionEndpointReference. ", e);
        }
    }

    // private but necessary properties for databinding
    @XmlElement(name = "Address", namespace = MemberSubmissionEndpointReference.MSNS)
    private Address addr;
    @XmlElement(name = "ReferenceProperties", namespace = MemberSubmissionEndpointReference.MSNS)
    private Element referenceParameters;
    @XmlElement(name = "ReferenceParameters", namespace = MemberSubmissionEndpointReference.MSNS)
    private Element metadata;

    @XmlType(name = "MemberSubmissionAddressType", namespace = MemberSubmissionEndpointReference.MSNS)
    private static class Address {
        @XmlValue
        String uri;
        @XmlAnyAttribute
        Map<QName, String> attributes;
    }

    @XmlType(name = "MemberSubmissionElementType", namespace = MemberSubmissionEndpointReference.MSNS)
    private static class Element {
        @XmlAnyElement
        List<Element> elements;
        @XmlAnyAttribute
        Map<QName, String> attributes;
    }


    protected static final String MSNS = "http://schemas.xmlsoap.org/ws/2004/08/addressing";
}
