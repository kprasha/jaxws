package com.sun.xml.ws.sandbox.message;

import com.sun.xml.ws.sandbox.XMLStreamWriterEx;
import com.sun.xml.ws.sandbox.Encoder;

import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import java.io.InputStream;
import java.lang.reflect.Proxy;

/**
 * Represents a SOAP message.
 *
 *
 * <h2>What is a message?</h2>
 * <p>
 * A {@link Message} consists of the following:
 *
 * <ol>
 * <li>
 *    Random-accessible list of headers.
 *    a header is a representation of an element inside
 *    &lt;soap:Header>.
 *    It can be read multiple times,
 *    can be added or removed, but it is not modifiable.
 *
 * <li>
 *    The payload of the message, which is a representation
 *    of an element inside &lt;soap:Body>.
 *    the payload is streamed, and therefore it can be
 *    only read once (or can be only written to something once.)
 *    once a payload is used, a message is said to be <b>consumed</b>.
 *
 * <li>
 *    Attachments.
 *    TODO: can attachments be streamed? I suspect so.
 *    does anyone need to read attachment twice?
 *
 * <li>
 *    Properties, which is a bag that keeps information
 *    that is not part of the SOAP message on the wire,
 *    yet relevant to the processing stacks inside JAX-WS.
 *
 * </ol>
 *
 *
 * <h2>How does this abstraction work?</h2>
 * <p>
 * The basic idea behind the {@link Message} is to hide the actual
 * data representation. For example, a {@link Message} might be
 * constructed on top of an {@link InputStream} from the accepted HTTP connection,
 * or it might be constructed on top of a JAXB object as a result
 * of the method invocation through {@link Proxy}. There will be
 * a {@link Message} implementation for each of those cases.
 *
 * <p>
 * This interface provides a lot of methods that access the payload
 * in many different forms, and implementations can implement those
 * methods in the best possible way.
 *
 * <p>
 * A particular attention is paid to make sure that a {@link Message}
 * object can be constructed on a stream that is not fully read yet.
 * We believe this improves the turn-around time on the server side.
 *
 * <p>
 * It is often useful to wrap a {@link Message} into another {@link Message},
 * for example to encrypt the body, or to verify the signature as the body
 * is read.
 *
 * <p>
 * This representation is also used for a REST-ful XML message.
 * In such case we'll construct a {@link Message} with empty
 * attachments and headers, and when serializing all headers
 * and attachments will be ignored.
 *
 *
 *
 * <h2>Message and XOP</h2>
 * <p>
 * XOP is considered as an {@link Encoder}, and therefore when you are looking at
 * {@link Message}, you'll never see &lt;xop:Include> or any such elements
 * (instead you'll see the base64 data inlined.)
 *
 *
 *
 * TODO: can body element have foreign attributes? maybe ID for security?
 *       Yes, when the SOAP body is signed there will be an ID attribute present
 *       But in this case any security based impl may need access
 *       to the concrete representation.
 * TODO: HTTP headers?
 *       Yes. Abstracted as transport-based properties.
 * TODO: who handles SOAP 1.1 and SOAP 1.2 difference?
 *       As separate channel implementations responsible for the creation of the
 *       message?
 * TODO: session?
 * TODO: Do we need to expose SOAPMessage explicitly?
 *       SOAPMessage could be the concrete representation but is it necessary to 
 *       transform between different concrete representations?
 *       Perhaps this comes down to how use channels for creation and processing.
 * TODO: Do we need to distinguish better between creation and processing?
 *       Do we really need the requirement that a created message can be resused 
 *       for processing. Shall we bifurcate?
 *
 * TODO: SOAP version issue
 *       SOAP version is determined by the context, so message itself doesn't carry it around (?)
 *
 * TODO: wrapping message needs easier. in particular properties and attachments.
 */
public abstract class Message {

    private final HeaderList headers;

    protected Message(HeaderList headers) {
        this.headers = headers;
    }

    protected Message() {
        this(new HeaderList());
    }

    /**
     * Gets all the headers of this message.
     *
     * @return
     *      always return the same non-null object.
     */
    public final HeaderList getHeaders() {
        return headers;
    }

    /**
     * Returns the properties of this message.
     *
     * @return
     *      always return the same object. Never null.
     */
    public abstract MessageProperties getProperties();

    /**
     * Gets the attachments of this message
     * (attachments live outside a message.)
     */
    public abstract AttachmentSet getAttachments();

    /**
     * Gets the local name of the payload element.
     */
    public abstract String getPayloadLocalPart();

    /**
     * Gets the namespace URI of the payload element.
     */
    public abstract String getPayloadNamespaceURI();

    /**
     * Returns true if this message is a fault.
     *
     * <p>
     * Just a convenience method built on {@link #getPayloadNamespaceURI()}
     * and {@link #getPayloadLocalPart()}.
     */
    public boolean isFault() {
        // TODO: is SOAP version a property of a Message?
        // or is it defined by external factors?
        // how do I compare?
        return getPayloadLocalPart()=="Fault"
            && getPayloadNamespaceURI()=="http://schemas.xmlsoap.org/soap/envelope/";

    }

    /**
     * Consumes this message including the envelope.
     * returns it as a {@link Source} object.
     */
    public abstract Source readEnvelopeAsSource();


    /**
     * Returns the payload as a {@link Source} object.
     *
     * This consumes the message.
     */
    public abstract Source readPayloadAsSource();

    /**
     * Creates the equivalent {@link SOAPMessage} from this message.
     *
     * This consumes the message.
     */
    public abstract SOAPMessage readAsSOAPMessage();

    /**
     * Reads the payload as a JAXB object by using the given unmarshaller.
     *
     * This consumes the message.
     */
    public abstract <T> T readAsJAXB(Unmarshaller unmarshaller);

    /**
     * Reads the payload as a {@link XMLStreamReader}
     *
     * This consumes the message.
     */
    public abstract XMLStreamReader readPayload();

    /**
     * Writes the payload to StAX.
     *
     * This method writes just the payload of the message to the writer.
     * This consumes the message.
     */
    public abstract void writePayloadTo(XMLStreamWriterEx sw);

    /**
     * Writes the whole SOAP message (but not attachments)
     * to the given writer.
     *
     * This consumes the message.
     */
    public abstract void writeTo(XMLStreamWriterEx sw);

    // TODO: do we need a method that reads payload as a fault?
    // do we want a separte streaming representation of fault?
    // or would SOAPFault in SAAJ do?
}
