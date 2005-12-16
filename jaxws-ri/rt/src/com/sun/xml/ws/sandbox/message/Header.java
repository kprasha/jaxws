package com.sun.xml.ws.sandbox.message;

import com.sun.xml.ws.sandbox.XMLStreamWriterEx;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


/**
 * A SOAP header.
 *
 * <p>
 * A header is read-only, but unlike body it can be read
 * multiple times (TODO: is this really necessary?)
 * The {@link Header} abstraction hides how the header
 * data is represented in memory; instead, it commits to
 * the ability to write itself to XML infoset.
 *
 * <p>
 * When a message is received from the transport and
 * being processed, the processor needs to "peek"
 * some information of a header, such as the tag name,
 * the mustUnderstand attribute, and so on. Therefore,
 * the {@link Header} interface exposes those information
 * as properties, so that they can be checked without
 * replaying the infoset, which is efficiently but still
 * costly.
 *
 * <p>
 * A {@link Header} may belong to more than one {@link HeaderList}
 * due to wrapping of {@link Message}.
 *
 * @see HeaderList
 */
public interface Header {
    // TODO: Vivek pointed out that the only time we are looking at
    // mustUnderstand and role are when we do the mustUnderstand error check
    // (that is, to find out if there's any header with @mustUnderstand that
    // has appropriate role for us.)
    // if that's the case, it might be better if we define this whole operation
    // as one method, instead of exposing two properties.

    /**
     * True if this header must be understood.
     */
    public boolean isMustUnderstood();

    /**
     * Gets the value of the soap:role attribute (or soap:actor for SOAP 1.1).
     *
     * <p>
     * SOAP 1.1 values are normalized into SOAP 1.2 values.
     *
     * An omitted SOAP 1.1 actor attribute value will become:
     * "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver"
     * An SOAP 1.1 actor attribute value of:
     * "http://schemas.xmlsoap.org/soap/actor/next"
     * will become:
     * "http://www.w3.org/2003/05/soap-envelope/role/next"
     *
     * <p>
     * If the soap:role attribute is absent, this method returns
     * "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver".
     *
     * @return
     *      never null. This string need not be interned.
     */
    public String getRole();

    /**
     * True if this header is to be relayed if not processed.
     * For SOAP 1.1 messages, this method always return false.
     *
     * <p>
     * IOW, this method returns true if there's @soap:relay='true'
     * is present.
     *
     * <h3>Implementation Note</h3>
     * <p>
     * The implementation needs to check for both "true" and "1",
     * but because attribute values are normalized, it doesn't have
     * to consider " true", " 1 ", and so on.
     *
     * @return
     *      false.
     */
    public boolean isRelay();

    /**
     * Gets the namespace URI of this header element.
     *
     * @return
     *      never null.
     *      this string must be interned.
     */
    public String getNamespaceURI();

    /**
     * Gets the local name of this header element.
     *
     * @return
     *      never null.
     *      this string must be interned.
     */
    public String getLocalPart();

    /**
     * Reads the header as a {@link XMLStreamReader}.
     *
     *
     * <h3>Performance Expectation</h3>
     * <p>
     * For some {@link Header} implementations, this operation
     * is a non-trivial operation. Therefore, use of this method
     * is discouraged unless the caller is interested in reading
     * the whole header.
     *
     * <p>
     * Similarly, if the caller wants to use this method only to do
     * the API conversion (such as simply firing SAX events from
     * {@link XMLStreamReader}), then the JAX-WS team requests
     * that you talk to us.
     *
     * @return
     *      must not null.
     */
    public XMLStreamReader readHeader() throws XMLStreamException;

    /**
     * Reads the header as a JAXB object by using the given unmarshaller.
     *
     */
    public <T> T readAsJAXB(Unmarshaller unmarshaller) throws JAXBException;

    /**
     * Writes out the header.
     *
     * @throws XMLStreamException
     *      if the operation fails for some reason. This leaves the
     *      writer to an undefined state.
     */
    public void writeTo(XMLStreamWriterEx w) throws XMLStreamException;

    /**
     * Writes out the header to the given SOAPMessage.
     *
     * <p>
     * Sometimes a {@link Message} needs to produce itself
     * as {@link SOAPMessage}, in which case each header needs
     * to turn itself into a header.
     *
     * @throws SOAPException
     *      if the operation fails for some reason. This leaves the
     *      writer to an undefined state.
     */
    public void writeTo(SOAPMessage saaj) throws SOAPException;

    /**
     * Writes out the header as SAX events.
     *
     * <p>
     * Sometimes a {@link Message} needs to produce SAX events,
     * and this method is necessary for headers to participate to it.
     *
     * <p>
     * A header is responsible for producing the SAX events for its part,
     * including <tt>startPrefixMapping</tt> and <tt>endPrefixMapping</tt>,
     * but not startDocument/endDocument.
     *
     * <p>
     * Note that SAX contract requires that any error that does NOT originate
     * from {@link ContentHandler} (meaning any parsing error and etc) must
     * be first reported to {@link ErrorHandler}. If the SAX event production
     * cannot be continued and the processing needs to abort, the code may
     * then throw the same {@link SAXParseException} reported to {@link ErrorHandler}.
     *
     * @param contentHandler
     *      The {@link ContentHandler} that receives SAX events.
     *
     * @param errorHandler
     *      The {@link ErrorHandler} that receives parsing errors.
     */
    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException;
}
