package com.sun.xml.ws.sandbox.fault;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * <pre>
 *    &lt;env:Text xml:lang="en">Sender Timeout</env:Text>
 * </pre>
 */
@XmlType(name = "TextType", namespace = "http://www.w3.org/2003/05/soap-envelope")
public class TextType {
    public
    @XmlValue
    String text;

    /**
     * xml:lang attribute. What should be value of namespace for "xml"
     */
    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace", required = true)
    public String lang;
}
