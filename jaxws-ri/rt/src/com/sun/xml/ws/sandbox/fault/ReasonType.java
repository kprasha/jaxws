package com.sun.xml.ws.sandbox.fault;

import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;
import java.util.ArrayList;

/**
 * <pre>
 * &lt;env:Reason>
 *     &lt;env:Text xml:lang="en">Sender Timeout</env:Text>
 * &lt;/env:Reason>
 * </pre>
 */
public class ReasonType {
    /**
     * minOccurs=1 maxOccurs=unbounded
     */
    @XmlElements(@XmlElement(name = "Text", namespace = "http://www.w3.org/2003/05/soap-envelope", type = TextType.class))
    public final List<TextType> text = new ArrayList<TextType>();
}
