package com.sun.xml.ws.api.message;

import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.sandbox.message.impl.jaxb.JAXBHeader11;
import com.sun.xml.ws.sandbox.message.impl.jaxb.JAXBHeader12;
import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;

import javax.xml.bind.Marshaller;

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
        switch(soapVersion) {
        case SOAP_11:
            return new JAXBHeader11(m,o);
        case SOAP_12:
            return new JAXBHeader12(m,o);
        default:
            throw new AssertionError();
        }
    }

    /**
     * Creates a {@link Header} backed a by a JAXB bean.
     */
    public static Header create(SOAPVersion soapVersion, Bridge bridge, BridgeContext bridgeInfo, Object jaxbObject) {
        switch(soapVersion) {
        case SOAP_11:
            return new JAXBHeader11(bridge, bridgeInfo, jaxbObject);
        case SOAP_12:
            return new JAXBHeader12(bridge, bridgeInfo, jaxbObject);
        default:
            throw new AssertionError();
        }
    }
}
