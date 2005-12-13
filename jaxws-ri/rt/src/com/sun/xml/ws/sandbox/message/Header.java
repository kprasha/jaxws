package com.sun.xml.ws.sandbox.message;

import com.sun.xml.ws.sandbox.XMLStreamWriterEx;

import javax.xml.bind.Unmarshaller;
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
 * @see HeaderList
 */
public interface Header {
    /**
     * True if this header must be understood.
     */
    public boolean isMustUnderstood();

    /**
     * Gets the value of the soap:role attribute (or soap:actor for SOAP 1.1).
     *
     * SOAP 1.1 values are normalized into SOAP 1.2 values.
     *
     * An omitted SOAP 1.1 actor attribute value will become:
     * "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver"
     * An SOAP 1.1 actor attribute value of:
     * "http://schemas.xmlsoap.org/soap/actor/next"
     * will become:
     * "http://www.w3.org/2003/05/soap-envelope/role/next"
     */
    public String getRole();

    /**
     * True if this header is to be relayed if not processed
     * (only supported for SOAP 1.2).
     */
    public boolean isRelay();
    
    /**
     * Gets the namespace URI of this header element.
     *
     * @return never null
     */
    public String getNamespaceURI();

    /**
     * Gets the local name of this header element.
     *
     * @return never null
     */
    public String getLocalPart();

    /**
     * Reads the header as a {@link XMLStreamReader}
     */
    public XMLStreamReader readHeader();

    /**
     * Reads the header as a JAXB object by using the given unmarshaller.
     *
     */
    public <T> T readAsJAXB(Unmarshaller unmarshaller);
    
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
     * TODO: justify why this is necessary
     *
     * @throws XMLStreamException
     *      if the operation fails for some reason. This leaves the
     *      writer to an undefined state.
     */
    public void writeTo(SOAPMessage saaj) throws XMLStreamException;
}
