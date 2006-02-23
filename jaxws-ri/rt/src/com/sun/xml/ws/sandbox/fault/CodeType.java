package com.sun.xml.ws.sandbox.fault;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.AccessType;
import javax.xml.namespace.QName;

/**
 * <pre>
 *  &lt;env:Code>
 *       &lt;env:Value>env:Sender&lt;/env:Value>
 *       &lt;env:Subcode>
 *           &lt;env:Value>m:MessageTimeout1&lt;/env:Value>
 *           &lt;env:Subcode>
 *               &lt;env:Value>m:MessageTimeout2&lt;/env:Value>
 *           &lt;/env:Subcode>
 *       &lt;/env:Subcode>
 *  &lt;/env:Code>
 * </pre>
 */
@XmlAccessorType(AccessType.FIELD)
@XmlType(name = "CodeType", namespace = "http://www.w3.org/2003/05/soap-envelope", propOrder = {
    "Value",
    "Subcode"
})
class CodeType {
    @XmlTransient
    private static final String ns="http://www.w3.org/2003/05/soap-envelope";

    /**
     * mandatory, minOccurs=1
     */
    @XmlElement(namespace = ns)
    private QName Value;

    /**
     * optional, minOcccurs=0, maxOccurs="1"
     */
    @XmlElement(namespace = ns)
    private SubcodeType Subcode;

    CodeType(QName value) {
        Value = value;
    }

    CodeType() {
    }

    QName getValue(){
        return Value;
    }

    SubcodeType getSubcode(){
        return Subcode;
    }

    void setSubcode(SubcodeType subcode) {
        Subcode = subcode;
    }
}
