package com.sun.xml.ws.sandbox.fault;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * &lt;env:Reason>
 *     &lt;env:Text xml:lang="en">Sender Timeout</env:Text>
 * &lt;/env:Reason>
 * </pre>
 */
class ReasonType {
    ReasonType() {
    }

    ReasonType(String txt) {
        text.add(new TextType(txt));
    }



    /**
     * minOccurs=1 maxOccurs=unbounded
     */
    @XmlElements(@XmlElement(name = "Text", namespace = "http://www.w3.org/2003/05/soap-envelope", type = TextType.class))
    private final List<TextType> text = new ArrayList<TextType>();

    List<TextType> texts() {
        return text;
    }
}
