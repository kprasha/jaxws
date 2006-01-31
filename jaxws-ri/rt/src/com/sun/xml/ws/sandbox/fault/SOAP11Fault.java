package com.sun.xml.ws.sandbox.fault;

import com.sun.xml.bind.api.TypeReference;

import javax.xml.bind.annotation.AccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

/**
 * This class represents SOAP1.1 Fault. This class will be used to marshall/unmarshall a soap fault using JAXB.
 * <p/>
 * <pre>
 * Example:
 * <p/>
 *     &lt;soap:Fault xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/' >
 *         &lt;faultcode>soap:Client&lt;/faultcode>
 *         &lt;faultstring>Invalid message format&lt;/faultstring>
 *         &lt;faultactor>http://example.org/someactor&lt;/faultactor>
 *         &lt;detail>
 *             &lt;m:msg xmlns:m='http://example.org/faults/exceptions'>
 *                 Test message
 *             &lt;/m:msg>
 *         &lt;/detail>
 *     &lt;/soap:Fault>
 * Above, m:msg, if a known fault (described in the WSDL), IOW, if m:msg is known by JAXBContext it should be unmarshalled into a
 * Java object otherwise it should be deserialized as {@link javax.xml.soap.Detail}
 * </pre>
 * <p/>
 * TODO: Add any missing annotation
 *
 * @author Vivek Pandey
 */
@XmlRootElement(name = "Fault", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
@XmlAccessorType(AccessType.FIELD)
@XmlType(name = "", propOrder = {
        "faultcode",
        "faultstring",
        "faultactor",
        "detail"
        })
public class SOAP11Fault extends SOAPFaultBuilder {
    @XmlElement(namespace = "")
    private String faultcode;

    @XmlElement(namespace = "")
    private String faultstring;

    @XmlElement(namespace = "")
    private String faultactor;
    /**
     * detail is a choice between {@link javax.xml.soap.Detail} and a JAXB object. Lets keep it as T or can be {@link Object} as well
     */
    @XmlElement(namespace = "")
    private DetailType detail;

    public SOAP11Fault() {
    }

    /**
     * This constructor takes soap fault detail among other things. The detail could represent {@link javax.xml.soap.Detail}
     * or a java object that can be marshalled/unmarshalled by JAXB.
     *
     * @param code
     * @param reason
     * @param actor
     * @param detail
     */
    public SOAP11Fault(String code, String reason, String actor, DetailType detail) {
        this.faultcode = code;
        this.faultstring = reason;
        this.faultactor = actor;
        this.detail = detail;
    }

    public String getFaultcode() {
        return faultcode;
    }

    public void setFaultcode(String faultcode) {
        this.faultcode = faultcode;
    }

    @Override
    public String getFaultString() {
        return faultstring;
    }

    public void setFaultstring(String faultstring) {
        this.faultstring = faultstring;
    }

    public String getFaultactor() {
        return faultactor;
    }

    public void setFaultactor(String faultactor) {
        this.faultactor = faultactor;
    }

    /**
     * returns a java type T - this could be a {@link javax.xml.soap.Detail} or a JAXB object
     */
    @Override
    public DetailType getDetail() {
        return detail;
    }

    /**
     * @param detail could be {@link javax.xml.soap.Detail} or a JAXB object
     */
    public void setDetail(DetailType detail) {
        this.detail = detail;
    }

    public static TypeReference getTypeReference() {
        return typeReference;
    }

    private static final TypeReference typeReference = new TypeReference(new QName("http://schemas.xmlsoap.org/soap/envelope/", "Fault"),
            SOAP11Fault.class, SOAP11Fault.class.getAnnotations());
}
