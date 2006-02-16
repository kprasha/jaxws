package com.sun.xml.ws.sandbox.fault;


import org.w3c.dom.Node;

import javax.xml.bind.annotation.AccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.Detail;
import javax.xml.soap.SOAPException;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.ws.WebServiceException;
import java.util.ArrayList;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.SOAPVersion;

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
class SOAP12Fault extends SOAPFaultBuilder {
    @XmlTransient
    private static final String ns = "http://www.w3.org/2003/05/soap-envelope";

    @XmlElement(namespace = ns)
    private CodeType Code;

    @XmlElement(namespace = ns)
    private ReasonType Reason;

    @XmlElement(namespace = ns)
    private String Node;

    @XmlElement(namespace = ns)
    private String Role;

    @XmlElement(namespace = ns)
    private DetailType Detail;

    SOAP12Fault() {
    }

    SOAP12Fault(CodeType code, ReasonType reason, String node, String role, DetailType detail) {
        Code = code;
        Reason = reason;
        Node = node;
        Role = role;
        Detail = detail;
    }

    SOAP12Fault(QName code, String reason, String actor, Node detailObject) {
        Code = new CodeType(code);
        Reason = new ReasonType(reason);
        if(detailObject != null)
            Detail = new DetailType(detailObject);
    }

    CodeType getCode() {
        return Code;
    }

    ReasonType getReason() {
        return Reason;
    }

    String getNode() {
        return Node;
    }

    String getRole() {
        return Role;
    }

    @Override
    DetailType getDetail() {
        return Detail;
    }
    @Override
    String getFaultString() {
        return Reason.texts().get(0).getText();
    }

     protected Throwable getProtocolException(Message msg) {
        try {
            SOAPFault fault = SOAPVersion.SOAP_12.saajSoapFactory.createFault(Reason.texts().get(0).getText(), (Code != null)?Code.getValue():null);
            if(Detail != null && Detail.getDetails() != null && Detail.getDetails().size() > 0 && Detail.getDetails().get(0) instanceof Detail){
                Node n = fault.getOwnerDocument().importNode((Detail)Detail.getDetails().get(0), true);
                fault.appendChild(n);
            }
            if(Code != null)
                fillFaultSubCodes(fault, Code.getSubcode());

            return new SOAPFaultException(fault);
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Recursively populate the Subcodes
     */
    private void fillFaultSubCodes(SOAPFault fault, SubcodeType subcode) throws SOAPException {
        if(subcode != null){
            fault.appendFaultSubcode(subcode.getValue());
            fillFaultSubCodes(fault, subcode.getSubcode());
        }
        return;
    }
}

