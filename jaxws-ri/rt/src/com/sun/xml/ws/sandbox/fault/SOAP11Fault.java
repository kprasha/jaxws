package com.sun.xml.ws.sandbox.fault;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.SOAPVersion;

import javax.xml.bind.annotation.AccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.Detail;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.Node;

/**
 * This class represents SOAP1.1 Fault. This class will be used to marshall/unmarshall a soap fault using JAXB.
 * <p/>
 * <pre>
 * Example:
 * <p/>
 *     &lt;soap:Fault xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>
 *         &lt;faultcode>soap:Client&lt;/faultcode>
 *         &lt;faultstring>Invalid message format&lt;/faultstring>
 *         &lt;faultactor>http://example.org/someactor&lt;/faultactor>
 *         &lt;detail>
 *             &lt;m:msg xmlns:m='http://example.org/faults/exceptions'>
 *                 Test message
 *             &lt;/m:msg>
 *         &lt;/detail>
 *     &lt;/soap:Fault>
 * <p/>
 * Above, m:msg, if a known fault (described in the WSDL), IOW, if m:msg is known by JAXBContext it should be unmarshalled into a
 * Java object otherwise it should be deserialized as {@link javax.xml.soap.Detail}
 * </pre>
 * <p/>
 *
 * @author Vivek Pandey
 */

@XmlAccessorType(AccessType.FIELD)
@XmlType(name = "", propOrder = {
        "faultcode",
        "faultstring",
        "faultactor",
        "detail"
        })
@XmlRootElement(name = "Fault", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
class SOAP11Fault extends SOAPFaultBuilder {
    @XmlElement(namespace = "")
    private QName faultcode;

    @XmlElement(namespace = "")
    private String faultstring;

    @XmlElement(namespace = "")
    private String faultactor;
    /**
     * detail is a choice between {@link javax.xml.soap.Detail} and a JAXB object. Lets keep it as T or can be {@link Object} as well
     */
    @XmlElement(namespace = "")
    private DetailType detail;

    SOAP11Fault() {
    }

    /**
     * This constructor takes soap fault detail among other things. The detail could represent {@link javax.xml.soap.Detail}
     * or a java object that can be marshalled/unmarshalled by JAXB.
     *
     * @param code
     * @param reason
     * @param actor
     * @param detailObject
     */
    SOAP11Fault(QName code, String reason, String actor, Object detailObject) {
        this.faultcode = code;
        this.faultstring = reason;
        this.faultactor = actor;
        if (detailObject != null) {
            detail = new DetailType(detailObject);
        }
    }

    QName getFaultcode() {
        return faultcode;
    }

    void setFaultcode(QName faultcode) {
        this.faultcode = faultcode;
    }

    @Override
    String getFaultString() {
        return faultstring;
    }

    void setFaultstring(String faultstring) {
        this.faultstring = faultstring;
    }

    String getFaultactor() {
        return faultactor;
    }

    void setFaultactor(String faultactor) {
        this.faultactor = faultactor;
    }

    /**
     * returns a java type T - this could be a {@link javax.xml.soap.Detail} or a JAXB object
     */
    @Override
    DetailType getDetail() {
        return detail;
    }

    /**
     * @param detail could be {@link javax.xml.soap.Detail} or a JAXB object
     */
    void setDetail(DetailType detail) {
        this.detail = detail;
    }

    protected Throwable getProtocolException(Message msg) {
        try {
            SOAPFault fault = SOAPVersion.SOAP_11.saajSoapFactory.createFault(faultstring, faultcode);
            if(detail != null && detail.getDetails() != null && detail.getDetails().size() > 0 && detail.getDetails().get(0) instanceof Node){
                Node n = fault.getOwnerDocument().importNode((Node)detail.getDetails().get(0), true);
                Detail d = fault.addDetail();
                d.appendChild(n);
            }
            fault.setFaultActor(faultactor);
            return new SOAPFaultException(fault);
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }
}
