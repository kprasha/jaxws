
package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferException;
import com.sun.xml.stream.buffer.XMLStreamBufferMark;
import com.sun.xml.stream.buffer.stax.StreamReaderBufferCreator;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamMessage;
import com.sun.xml.ws.sandbox.message.impl.EmptyMessageImpl;
import com.sun.xml.ws.streaming.XMLStreamReaderUtil;
import com.sun.xml.ws.streaming.XMLStreamReaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamConstants;

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

    // consider caching
    // private final XMLStreamReader reader;

    public Packet decode(InputStream in, String contentType) throws IOException {
        XMLStreamReader reader = createXMLStreamReader(in);

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
            if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
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
            return new Packet(createMessage(headers, reader));
        } else {
            // Empty payload <soap:Body/>
            return new Packet(new EmptyMessageImpl(headers, SOAPVersion.fromNsUri(SOAP_NAMESPACE_URI)));
        }
    }

    /**
     *
     * @see #decode(InputStream, String)
     */
    public Packet decode(ReadableByteChannel in, String contentType ) {
        throw new UnsupportedOperationException();
    }

    public final Decoder copy() {
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
            Map<String,String> headerBlockNamespaces = namespaces;

            // Collect namespaces on SOAP header block
            if (reader.getNamespaceCount() > 0) {
                headerBlockNamespaces = new HashMap<String,String>(namespaces);
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

    private XMLStreamReader createXMLStreamReader(InputStream in) {
        // TODO: we should definitely let Decode owns one XMLStreamReader instance
        // instead of going to this generic factory
        return XMLStreamReaderFactory.createXMLStreamReader(in,true);
    }

    private XMLStreamBuffer createXMLStreamBuffer() {
        return new XMLStreamBuffer();
    }


    /**
     * Creates a new {@link StreamSOAPDecoder} instance.
     */
    public static Decoder create(SOAPVersion version) {
        if(version==null)
            // this decoder is for SOAP, not for XML/HTTP
            throw new IllegalArgumentException();
        switch(version) {
        case SOAP_11:
            return new StreamSOAP11Decoder();
        case SOAP_12:
            return new StreamSOAP12Decoder();
        default:
            throw new AssertionError();
        }
    }
}
