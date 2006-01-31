package com.sun.xml.ws.sandbox.fault;


import com.sun.xml.bind.api.TypeReference;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.AccessType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;

/**
 * SOAP 1.2 Fault class that can be marshalled/unmarshalled by JAXB
 * <p/>
 * <pre>
 * Example:
 * &lt;env:Envelope xmlns:env="http://www.w3.org/2003/05/soap-envelope"
 *            xmlns:m="http://www.example.org/timeouts"
 *            xmlns:xml="http://www.w3.org/XML/1998/namespace">
 * &lt;env:Body>
 *     &lt;env:Fault>
 *         &lt;env:Code>
 *             &lt;env:Value>env:Sender* &lt;/env:Value>
 *             &lt;env:Subcode>
 *                 &lt;env:Value>m:MessageTimeout* &lt;/env:Value>
 *             &lt;/env:Subcode>
 *         &lt;/env:Code>
 *         &lt;env:Reason>
 *             &lt;env:Text xml:lang="en">Sender Timeout* &lt;/env:Text>
 *         &lt;/env:Reason>
 *         &lt;env:Detail>
 *             &lt;m:MaxTime>P5M* &lt;/m:MaxTime>
 *         &lt;/env:Detail>
 *     &lt;/env:Fault>
 * &lt;/env:Body>
 * &lt;/env:Envelope>
 * </pre>
 *
 * @author Vivek Pandey
 */
@XmlRootElement(name = "Fault", namespace = "http://www.w3.org/2003/05/soap-envelope")
@XmlAccessorType(AccessType.FIELD)
@XmlType(name = "", propOrder = {
    "Code",
    "Reason",
    "Node",
    "Role",
    "Detail"
})
public class SOAP12Fault extends SOAPFaultBuilder {
    @XmlTransient
    public static final String ns = "http://www.w3.org/2003/05/soap-envelope";

    @XmlElement(namespace = ns)
    public CodeType Code;

    @XmlElement(namespace = ns)
    public ReasonType Reason;

    //@XmlElement(namespace = ns, nillable = true)
    public String Node;

    //@XmlElement(namespace = ns, nillable = true)
    public String Role;

    //@XmlElement(namespace = ns, nillable = true)
    public DetailType Detail;

    @Override
    public DetailType getDetail() {
        return Detail;
    }
    @Override
    public String getFaultString() {
        return Reason.text.get(0).text;
    }

    public static TypeReference getTypeReference() {
        return typeReference;
    }

    private static final TypeReference typeReference = new TypeReference(new QName("http://schemas.xmlsoap.org/soap/envelope/", "Fault"),
            SOAP11Fault.class, SOAP11Fault.class.getAnnotations());

}

