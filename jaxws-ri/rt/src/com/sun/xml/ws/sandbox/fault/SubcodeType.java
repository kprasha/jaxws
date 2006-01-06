package com.sun.xml.ws.sandbox.fault;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.AccessType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;

/**
 * <pre>
 *      &lt;env:Subcode>
 *          &lt;env:Value>m:MessageTimeout1&lt;/env:Value>
 *          &lt;env:Subcode>
 *              &lt;env:Value>m:MessageTimeout2&lt;/env:Value>
 *          &lt;/env:Subcode>
 *      &lt;/env:Subcode>
 *  </pre>
 */
@XmlAccessorType(AccessType.FIELD)
@XmlType(name = "SubcodeType", namespace = "http://www.w3.org/2003/05/soap-envelope", propOrder = {
    "Value",
    "Subcode"
        })
public class SubcodeType {
    @XmlTransient
    private static final String ns="http://www.w3.org/2003/05/soap-envelope";
    /**
     * mandatory, minOccurs=1
     */
    @XmlElement(namespace = ns)
    public QName Value;

    /**
     * optional, minOcccurs=0
     */
    @XmlElements(@XmlElement(namespace = ns))
    public SubcodeType Subcode;
}
