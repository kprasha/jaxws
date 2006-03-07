package com.sun.xml.ws.api.message;

import com.sun.istack.NotNull;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.sandbox.fault.SOAPFaultBuilder;
import com.sun.xml.ws.sandbox.impl.StreamSOAPDecoder;
import com.sun.xml.ws.sandbox.message.impl.DOMMessage;
import com.sun.xml.ws.sandbox.message.impl.EmptyMessageImpl;
import com.sun.xml.ws.sandbox.message.impl.jaxb.JAXBMessage;
import com.sun.xml.ws.sandbox.message.impl.saaj.SAAJMessage;
import com.sun.xml.ws.sandbox.message.impl.source.PayloadSourceMessage;
import com.sun.xml.ws.sandbox.message.impl.source.ProtocolSourceMessage;
import com.sun.xml.ws.streaming.XMLStreamReaderException;
import com.sun.xml.ws.streaming.XMLStreamReaderUtil;
import com.sun.xml.ws.util.DOMUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.ws.WebServiceException;

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
     * Creates a {@link Message} using {@link Source} as payload.
     *
     * @param payload
     *      Source payload is {@link Message}'s payload
     *      Must not be null. Once this method is invoked, the created
     *      {@link Message} will own the {@link Source}, so it shall
     *      never be touched directly.
     *
     * @param ver
     *      The SOAP version of the message. Must not be null.
     */
    public static Message createUsingPayload(Source payload, SOAPVersion ver) {
        return new PayloadSourceMessage(payload, ver);
    }

    /**
     * Creates a {@link Message} from an {@link Element} that represents
     * a payload.
     *
     * @param payload
     *      The element that becomes the child element of the SOAP body.
     *      Must not be null.
     *
     * @param ver
     *      The SOAP version of the message. Must not be null.
     */
    public static Message createUsingPayload(Element payload, SOAPVersion ver) {
        return new DOMMessage(ver,payload);
    }

    /**
     * Creates a {@link Message} from an {@link Element} that represents
     * the whole SOAP message.
     *
     * @param soapEnvelope
     *      The SOAP envelope element.
     */
    public static Message create(Element soapEnvelope) {
        SOAPVersion ver = SOAPVersion.fromNsUri(soapEnvelope.getNamespaceURI());
        // find the headers
        Element header = DOMUtil.getFirstChild(soapEnvelope, ver.nsUri, "Header");
        HeaderList headers = null;
        if(header!=null) {
            for( Node n=header.getFirstChild(); n!=null; n=n.getNextSibling() ) {
                if(n.getNodeType()==Node.ELEMENT_NODE) {
                    if(headers==null)
                        headers = new HeaderList();
                    headers.add(Headers.create(ver,(Element)n));
                }
            }
        }

        // find the payload
        Element body = DOMUtil.getFirstChild(soapEnvelope, ver.nsUri, "Body");
        if(body==null)
            throw new WebServiceException("Message doesn't have <S:Body> "+soapEnvelope);
        Element payload = DOMUtil.getFirstChild(soapEnvelope, ver.nsUri, "Body");

        if(payload==null) {
            return new EmptyMessageImpl(headers,ver);
        } else {
            return new DOMMessage(ver,headers,payload);
        }
    }

    /**
     * Creates a {@link Message} using Source as entire envelope.
     *
     * @param envelope
     *      Source envelope is used to create {@link Message}
     *      Must not be null. Once this method is invoked, the created
     *      {@link Message} will own the {@link Source}, so it shall
     *      never be touched directly.
     *
     */
    public static Message create(Source envelope) {
        // TODO: Doesn't the ProtocolSourceMessage require SOAPVersion
        return new ProtocolSourceMessage(envelope);
    }


    /**
     * Creates a {@link Message} that doesn't have any payload.
     */
    public static Message createEmpty(SOAPVersion soapVersion) {
        return new EmptyMessageImpl(soapVersion);
    }

    /**
     * Creates a {@link Message} from {@link XMLStreamReader} that points to
     * the start of the envelope.
     *
     * @param reader
     *      can point to the start document or the start element (of &lt;s:Envelope>)
     */
    public static @NotNull Message create(@NotNull XMLStreamReader reader) {
        // skip until the root element
        if(reader.getEventType()!=XMLStreamConstants.START_ELEMENT)
            XMLStreamReaderUtil.nextElementContent(reader);
        assert reader.getEventType()== XMLStreamConstants.START_ELEMENT;

        SOAPVersion ver = SOAPVersion.fromNsUri(reader.getNamespaceURI());

        return StreamSOAPDecoder.create(ver).decode(reader);
    }

    /**
     * Creates a {@link Message} from {@link XMLStreamBuffer} that retains the
     * whole envelope infoset.
     *
     * @param xsb
     *      This buffer must contain the infoset of the whole envelope.
     */
    public static @NotNull Message create(@NotNull XMLStreamBuffer xsb) {
        // TODO: we should be able to let Messae know that it's working off from a buffer,
        // to make some of the operations more efficient.
        // meanwhile, adding this as an API so that our users can take advantage of it
        // when we get around to such an implementation later.
        try {
            return create(xsb.processUsingXMLStreamReader());
        } catch (XMLStreamException e) {
            throw new XMLStreamReaderException(e);
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
    public static Message create(Throwable t, SOAPVersion soapVersion) {
        return SOAPFaultBuilder.createSOAPFaultMessage(soapVersion, null, t);
    }

    /**
     * Creates a fault {@link Message}.
     *
     * <p>
     * This method is not designed for efficiency, and we don't expect
     * to be used for the performance critical codepath.
     *
     * @param fault
     *      The populated SAAJ data structure that represents a fault
     *      in detail.
     *
     * @return
     *      Always non-null. A message that wraps this {@link SOAPFault}.
     */
    public static Message create(SOAPFault fault) {
        SOAPVersion ver = SOAPVersion.fromNsUri(fault.getNamespaceURI());
        return new DOMMessage(ver,fault);
    }
}
