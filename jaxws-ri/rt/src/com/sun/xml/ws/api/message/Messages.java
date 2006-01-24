package com.sun.xml.ws.api.message;

import com.sun.xml.ws.sandbox.message.impl.jaxb.JAXBMessage;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.pipe.Pipe;

import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Factory methods for various {@link Message} implementations.
 *
 * <p>
 * This class provides various methods to create different
 * flavors of {@link Message} classes that store data
 * in different formats.
 *
 * <p>
 * This is a part of the JAX-WS RI internal API so that
 * {@link Pipe} implementations can reuse the implementations
 * done inside the JAX-WS.
 *
 * <p>
 * If you find some of the useful convenience methods missing
 * from this class, please talk to us.
 *
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Messages {
    private Messages() {}

    /**
     * Creates a {@link Message} backed by a JAXB bean.
     *
     * @param marshaller
     *      The marshaller to be used to produce infoset from the object. Must not be null.
     * @param jaxbObject
     *      The JAXB object that represents the payload. must not be null. This object
     *      must be bound to an element (which means it either is a {@link JAXBElement} or
     *      an instanceof a class with {@link XmlRootElement}).
     * @param soapVersion
     *      The SOAP version of the message. Must not be null.
     */
    public static Message createJAXBMessage(Marshaller marshaller, Object jaxbObject, SOAPVersion soapVersion) {
        return new JAXBMessage(marshaller,jaxbObject,soapVersion);
    }
}
