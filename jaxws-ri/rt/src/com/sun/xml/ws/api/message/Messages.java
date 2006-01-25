package com.sun.xml.ws.api.message;

import com.sun.xml.ws.sandbox.message.impl.jaxb.JAXBMessage;
import com.sun.xml.ws.sandbox.message.impl.saaj.SAAJMessage;
import com.sun.xml.ws.sandbox.message.impl.EmptyMessageImpl;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.pipe.Pipe;

import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;

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
    public static Message create(Marshaller marshaller, Object jaxbObject, SOAPVersion soapVersion) {
        return new JAXBMessage(marshaller,jaxbObject,soapVersion);
    }

    /**
     * Creates a {@link Message} backed by a SAAJ {@link SOAPMessage} object.
     *
     * <p>
     * If the {@link SOAPMessage} contains headers and attachments, this method
     * does the right thing.
     *
     * @param saaj
     *      The SOAP message to be represented as a {@link Message}.
     *      Must not be null. Once this method is invoked, the created
     *      {@link Message} will own the {@link SOAPMessage}, so it shall
     *      never be touched directly.
     */
    public static Message create(SOAPMessage saaj) {
        return new SAAJMessage(saaj);
    }

    /**
     * Creates a {@link Message} that doesn't have any payload.
     */
    public static Message createEmpty(SOAPVersion soapVersion) {
        // TODO: fully implement EmptyMessageImpl
        Message msg = new EmptyMessageImpl();

        // for now we'll use an empty SAAJMessage
        try {
            return create(soapVersion.saajFactory.createMessage());
        } catch (SOAPException e) {
            // impossible
            throw new AssertionError(e);
        }
    }

    /**
     * Creates a {@link Message} that represents an exception as a fault.
     *
     * <p>
     * TODO: I'm not too sure if JAX-WS can represent any exception
     * as a fault --- it probably can't. So the exact signature needs to be
     * worked out. This method is here just to show the concept of
     * {@link Messages}.
     */
    public static Message create(Throwable t) {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }
}
