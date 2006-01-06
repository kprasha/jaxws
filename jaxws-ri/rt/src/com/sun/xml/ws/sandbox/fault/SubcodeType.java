package com.sun.xml.ws.sandbox.fault;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
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
@XmlType(name = "SubcodeType", namespace = "http://www.w3.org/2003/05/soap-envelope")
public class SubcodeType {
    private static final String ns="http://www.w3.org/2003/05/soap-envelope";
    /**
     * mandatory, minOccurs=1
     */
    @XmlElement(name = "Value", namespace = ns, type = QName.class)
    public QName value;

    /**
     * optional, minOcccurs=0
     */
    @XmlElements(@XmlElement(name = "Subcode", namespace = ns, type = SubcodeType.class, nillable = true))
    public SubcodeType subcode;
}
