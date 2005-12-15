package com.sun.xml.ws.sandbox.message.impl.stream;

import com.sun.xml.stream.buffer.XMLStreamBufferMark;
import javax.xml.soap.SOAPConstants;
import javax.xml.stream.XMLStreamReader;

/**
 * {@link StreamHeader} for SOAP 1.2.
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class StreamHeader12 extends StreamHeader {

    public StreamHeader12(XMLStreamReader reader, XMLStreamBufferMark mark) {
        super(reader, mark);
    }
    
    protected final void processHeaderAttributes(XMLStreamReader reader) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final String localName = reader.getAttributeLocalName(i);
            final String namespaceURI = reader.getAttributeNamespace(i);

            if (namespaceURI == SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE) {
                if (localName == SOAP_1_2_MUST_UNDERSTAND) {
                    _isMustUnderstand = convertToBoolean(reader.getAttributeValue(i));
                } else if (localName == SOAP_1_2_ROLE) {
                    final String value = reader.getAttributeValue(i);
                    if (value != null && value.length() > 0) {
                        _role = value;
                    }
                } else if (localName == SOAP_1_2_RELAY) {
                    _isRelay = convertToBoolean(reader.getAttributeValue(i));
                }
            }
        }
    }
        
}
