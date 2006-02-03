package com.sun.xml.ws.sandbox.fault;

import org.w3c.dom.Node;

import javax.xml.bind.annotation.XmlAnyElement;
import java.util.ArrayList;
import java.util.List;

/**
 * &lt;env:Detail>
 *     &lt;m:MaxTime>P5M</m:MaxTime>
 * &lt;/env:Detail>
 */
class DetailType {
    /**
     * TODO: annotate 0 or more attriubtes
     */
    private List<Object> attributes;

    /**
     * The detail entry could be 0 or more elements. Perhaps some elements may be
     * known to JAXB while others can be handled using DOMHandler.
     *
     * Even though the jaxbContext is aware of the detail jaxbBean but we get the list of
     * {@link org.w3c.dom.Node}s.
     *
     * this is because since we unmarshall using {@link com.sun.xml.bind.api.Bridge} all we're
     * going to get during unmarshalling is {@link org.w3c.dom.Node} and not the jaxb bean instances.
     *
     * TODO: For now detailEntry would be List of Node isntead of Object and it needs to be changed to
     * {@link Object} once we have better solution that working thru {@link com.sun.xml.bind.api.Bridge}
     */
    @XmlAnyElement(lax=true)
    private List<Object> detailEntry;

    List<Object> getDetails() {
        return detailEntry;
    }

    DetailType(Object detailObject) {
        if(detailObject != null){
            detailEntry = new ArrayList<Object>();
            detailEntry.add(detailObject);
        }
    }

    DetailType() {
    }
}
