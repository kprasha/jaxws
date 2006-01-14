
package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferException;
import com.sun.xml.stream.buffer.XMLStreamBufferMark;
import com.sun.xml.stream.buffer.stax.StreamReaderBufferCreator;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamMessage;
import com.sun.xml.ws.streaming.XMLStreamReaderUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * A stream SOAP decoder.
 *
 * @author Paul Sandoz
 */
public abstract class StreamSOAPDecoder implements Decoder {
    
    private static final String SOAP_ENVELOPE = "Envelope";
    private static final String SOAP_HEADER = "Header";
    private static final String SOAP_BODY = "Body";

    protected final String SOAP_NAMESPACE_URI;
    
    protected StreamSOAPDecoder(String namespace) {
        SOAP_NAMESPACE_URI = namespace;
    }
    
    public Message decode(InputStream in, String contentType) throws IOException {
        XMLStreamReader reader = createXMLStreamReader();

        // Check at the start of the document
        XMLStreamReaderUtil.verifyReaderState(reader,
                javax.xml.stream.XMLStreamConstants.START_DOCUMENT);
        
        // Move to soap:Envelope and verify
        XMLStreamReaderUtil.nextElementContent(reader);
        XMLStreamReaderUtil.verifyReaderState(reader,
                javax.xml.stream.XMLStreamConstants.START_ELEMENT);
        XMLStreamReaderUtil.verifyTag(reader, SOAP_NAMESPACE_URI, SOAP_ENVELOPE);

        // Collect namespaces on soap:Envelope
        Map<String, String> namespaces = new HashMap();
        for (int i = 0; i < reader.getNamespaceCount(); i++) {
            namespaces.put(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
        }

        // Move to next element
        XMLStreamReaderUtil.nextElementContent(reader);
        XMLStreamReaderUtil.verifyReaderState(reader,
                javax.xml.stream.XMLStreamConstants.START_ELEMENT);

        HeaderList headers = null;
        if (reader.getLocalName() == SOAP_HEADER 
                && reader.getNamespaceURI() == SOAP_NAMESPACE_URI) {

            // Collect namespaces on soap:Header
            for (int i = 0; i < reader.getNamespaceCount(); i++) {
                namespaces.put(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
            }

            XMLStreamReaderUtil.nextElementContent(reader);
            
            // If SOAP header blocks are present (i.e. not <soap:Header/>)
            if (reader.getEventType() == javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
                headers = new HeaderList();
                
                try {
                    // Cache SOAP header blocks
                    cacheHeaders(reader, namespaces, headers);
                } catch (Exception e) {
                    // TODO need to throw more meaningful exception
                    throw new IOException("");
                }
            }
        }
    
        // Verify that <soap:Body> is present
        XMLStreamReaderUtil.verifyTag(reader, SOAP_NAMESPACE_URI, SOAP_BODY);
        
        // TODO: Cache attributes on body
        
        XMLStreamReaderUtil.nextElementContent(reader);
        if (reader.getEventType() == javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
            // Payload is present
            // XMLStreamReader is positioned at the first child
            return createMessage(headers, reader);
        } else {
            // Empty payload <soap:Body/>
            return createMessage(headers, null);
        } 
    }

    /**
     *
     * @see #decode(InputStream, String)
     */
    public Message decode(ReadableByteChannel in, String contentType ) {
        throw new UnsupportedOperationException();
    }

    public Decoder copy() {
        // TODO: when you make Decoder stateful, implement the copy method.
        return this;
    }

    private XMLStreamBuffer cacheHeaders(XMLStreamReader reader,
                                         Map<String, String> namespaces, HeaderList headers) throws XMLStreamException, XMLStreamBufferException {
        XMLStreamBuffer buffer = createXMLStreamBuffer();
        StreamReaderBufferCreator creator = new StreamReaderBufferCreator();
        creator.setXMLStreamBuffer(buffer);

        // Reader is positioned at the first header block
        while(reader.getEventType() == javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
            Map<String, String> headerBlockNamespaces = namespaces;

            // Collect namespaces on SOAP header block
            if (reader.getNamespaceCount() > 0) {
                headerBlockNamespaces = new HashMap();
                headerBlockNamespaces.putAll(namespaces);
                for (int i = 0; i < reader.getNamespaceCount(); i++) {
                    headerBlockNamespaces.put(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
                }
            }

            // Mark
            XMLStreamBufferMark mark = new XMLStreamBufferMark(headerBlockNamespaces, creator);
            // Create Header
            headers.add(createHeader(reader, mark));

            // Cache the header block
            // After caching Reader will be positioned at next header block or
            // the end of the </soap:header>
            creator.createElementFragment(reader, false);
        }

        // Move to soap:Body
        XMLStreamReaderUtil.nextElementContent(reader);

        return buffer;
    }
    
    protected abstract StreamHeader createHeader(XMLStreamReader reader, XMLStreamBufferMark mark);
    
    protected abstract StreamMessage createMessage(HeaderList headers, XMLStreamReader reader);

    private XMLStreamReader createXMLStreamReader() {
        // TODO Get reader from factory based on content type
        throw new UnsupportedOperationException();
    }
    
    private XMLStreamBuffer createXMLStreamBuffer() {
        // TODO Get unused buffer
        throw new UnsupportedOperationException();
    }    
}
