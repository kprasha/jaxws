package com.sun.xml.ws.sandbox.fault;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlMixed;
import java.util.List;

/**
 * &lt;env:Detail>
 *     &lt;m:MaxTime>P5M</m:MaxTime>
 * &lt;/env:Detail>
 */
public class DetailType {
    /**
     * TODO: annotate 0 or more attriubtes
     */
    public List<Object> attributes;

    /**
     * The detail entry could be 0 or more elements. Perhaps some elements may be
     * known to JAXB while others can be handled using DOMHandler.
     */
    @XmlAnyElement
    @XmlMixed
    public List<Object> detailEntry;
}
