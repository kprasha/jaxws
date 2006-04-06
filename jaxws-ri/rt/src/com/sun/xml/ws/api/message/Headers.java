package com.sun.xml.ws.api.message;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.stream.buffer.XMLStreamBufferException;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.sandbox.message.impl.DOMHeader;
import com.sun.xml.ws.sandbox.message.impl.jaxb.JAXBHeader;
import com.sun.xml.ws.sandbox.message.impl.saaj.SAAJHeader;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader11;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader12;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Factory methods for various {@link Header} implementations.
 *
 * <p>
 * This class provides various methods to create different
 * flavors of {@link Header} classes that store data
 * in different formats.
 *
 * <p>
 * This is a part of the JAX-WS RI internal API so that
 * {@link Pipe} implementations can reuse the implementations
 * done inside the JAX-WS without having a strong dependency
 * to the actual class.
 *
 * <p>
 * If you find some of the useful convenience methods missing
 * from this class, please talk to us.
 *
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Headers {
    private Headers() {}

    /**
     * Creates a {@link Header} backed a by a JAXB bean.
     */
    public static Header create(SOAPVersion soapVersion, Marshaller m, Object o) {
        return new JAXBHeader(m,o);
    }

    /**
     * Creates a {@link Header} backed a by a JAXB bean, with the given tag name.
     *
     * See {@link #create(SOAPVersion, Marshaller, Object)} for the meaning
     * of other parameters.
     *
     * @param tagName
     *      The name of the newly created header. Must not be null.
     * @param o
     *      The JAXB bean that represents the contents of the header. Must not be null.
     */
    public static Header create(SOAPVersion soapVersion, Marshaller m, QName tagName, Object o) {
        return create(soapVersion,m,new JAXBElement(tagName,o.getClass(),o));
    }

    /**
     * Creates a {@link Header} backed a by a JAXB bean.
     */
    public static Header create(SOAPVersion soapVersion, Bridge bridge, BridgeContext bridgeInfo, Object jaxbObject) {
        return new JAXBHeader(bridge, bridgeInfo, jaxbObject);
    }

    /**
     * Creates a new {@link Header} backed by a SAAJ object.
     */
    public static Header create(SOAPHeaderElement header) {
        return new SAAJHeader(header);
    }

    /**
     * Creates a new {@link Header} backed by an {@link Element}.
     */
    public static Header create( SOAPVersion soapVersion, Element node ) {
        return new DOMHeader<Element>(node);
    }

    /**
     * Creates a new {@link Header} that reads from {@link XMLStreamReader}.
     *
     * <p>
     * Note that the header implementation will read the entire data
     * into memory anyway, so this might not be as efficient as you might hope.
     */
    public static Header create( SOAPVersion soapVersion, XMLStreamReader reader ) throws XMLStreamBufferException, XMLStreamException {
        switch(soapVersion) {
        case SOAP_11:
            return new StreamHeader11(reader);
        case SOAP_12:
            return new StreamHeader12(reader);
        default:
            throw new AssertionError();
        }
    }
}
