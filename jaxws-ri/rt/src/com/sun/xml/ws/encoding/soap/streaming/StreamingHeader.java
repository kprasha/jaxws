package com.sun.xml.ws.encoding.soap.streaming;

import com.sun.xml.ws.sandbox.message.Header;

/**
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class StreamingHeader implements Header {
    
    protected final boolean _isMustUnderstand;
    
    protected final String _actor;
    
    protected final boolean _relay;
    
    /** Creates a new instance of StreamingHeader */
    public StreamingHeader() {
    }
    
    /**
     * True if this header must be understood.
     */
    public boolean isMustUnderstood();

    /**
     * Gets the value of the soap:actor attribute (or soap:role for SOAP 1.2), or null.
     */
    public String getRole();

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
     */
    public void writeTo(XMLStreamWriterEx w) throws XMLStreamException;

    /**
     * Writes out the header to the given SOAPMessage.
     *
     * TODO: justify why this is necessary
     */
    public void writeTo(SOAPMessage saaj);
    
}
