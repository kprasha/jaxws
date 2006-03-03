package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferException;
import com.sun.xml.stream.buffer.XMLStreamBufferMark;
import com.sun.xml.stream.buffer.stax.StreamReaderBufferCreator;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.sandbox.message.impl.EmptyMessageImpl;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamMessage;
import com.sun.xml.ws.streaming.XMLStreamReaderFactory;
import com.sun.xml.ws.streaming.XMLStreamReaderUtil;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * A stream SOAP decoder.
 *
 * @author Paul Sandoz
 */
@SuppressWarnings({"StringEquality"})
public abstract class StreamSOAPDecoder implements Decoder {

    private static final String SOAP_ENVELOPE = "Envelope";
    private static final String SOAP_HEADER = "Header";
    private static final String SOAP_BODY = "Body";

    private final String SOAP_NAMESPACE_URI;
    private final SOAPVersion soapVersion;

    /*package*/ StreamSOAPDecoder(SOAPVersion soapVersion) {
        SOAP_NAMESPACE_URI = soapVersion.nsUri;
        this.soapVersion = soapVersion;
    }

    // consider caching
    // private final XMLStreamReader reader;

    // consider caching
    // private final XMLStreamBuffer buffer;

    public void decode(InputStream in, String contentType, Packet packet) throws IOException {
        XMLStreamReader reader = createXMLStreamReader(in);
        packet.setMessage(decode(reader));
    }

    /**
     * Decodes a message from {@link XMLStreamReader} that points to
     * the beginning of a SOAP infoset.
     *
     * @param reader
     *      can point to the start document or the start element.
     */
    public final Message decode(XMLStreamReader reader) {

        // Move to soap:Envelope and verify
        if(reader.getEventType()!=XMLStreamConstants.START_ELEMENT)
            XMLStreamReaderUtil.nextElementContent(reader);
        XMLStreamReaderUtil.verifyReaderState(reader,XMLStreamConstants.START_ELEMENT);
        XMLStreamReaderUtil.verifyTag(reader, SOAP_NAMESPACE_URI, SOAP_ENVELOPE);

        TagInfoset envelopeTag = new TagInfoset(reader);

        // Collect namespaces on soap:Envelope
        Map<String,String> namespaces = new HashMap<String,String>();

        // Move to next element
        XMLStreamReaderUtil.nextElementContent(reader);
        XMLStreamReaderUtil.verifyReaderState(reader,
                javax.xml.stream.XMLStreamConstants.START_ELEMENT);

        HeaderList headers = null;
        TagInfoset headerTag = null;

        if (reader.getLocalName() == SOAP_HEADER
                && reader.getNamespaceURI() == SOAP_NAMESPACE_URI) {
            headerTag = new TagInfoset(reader);

            // Collect namespaces on soap:Header
            for(int i=0; i< reader.getNamespaceCount();i++){
                namespaces.put(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
            }
            // skip <soap:Header>
            XMLStreamReaderUtil.nextElementContent(reader);

            // If SOAP header blocks are present (i.e. not <soap:Header/>)
            if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
                headers = new HeaderList();

                try {
                    // Cache SOAP header blocks
                    cacheHeaders(reader, namespaces, headers);
                } catch (XMLStreamException e) {
                    // TODO need to throw more meaningful exception
                    throw new WebServiceException(e);
                } catch (XMLStreamBufferException e) {
                    throw new WebServiceException(e);
                }
            }

            // Move to soap:Body
            XMLStreamReaderUtil.nextElementContent(reader);
        }

        // Verify that <soap:Body> is present
        XMLStreamReaderUtil.verifyTag(reader, SOAP_NAMESPACE_URI, SOAP_BODY);
        TagInfoset bodyTag = new TagInfoset(reader);

        XMLStreamReaderUtil.nextElementContent(reader);
        if (reader.getEventType() == javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
            // Payload is present
            // XMLStreamReader is positioned at the first child
            return new StreamMessage(envelopeTag,headerTag,headers,bodyTag,reader,soapVersion);
        } else {
            // Empty payload <soap:Body/>
            return new EmptyMessageImpl(headers, SOAPVersion.fromNsUri(SOAP_NAMESPACE_URI));
        }
    }

    public void decode(ReadableByteChannel in, String contentType, Packet packet ) {
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

        return buffer;
    }

    protected abstract StreamHeader createHeader(XMLStreamReader reader, XMLStreamBufferMark mark);

    protected XMLStreamReader createXMLStreamReader(InputStream in) {
        // TODO: we should definitely let Decode owns one XMLStreamReader instance
        // instead of going to this generic factory
        return XMLStreamReaderFactory.createXMLStreamReader(in,true);
    }

    private XMLStreamBuffer createXMLStreamBuffer() {
        // TODO: Decode should own one XMLStreamBuffer for reuse
        // since it is more efficient. ISSUE: possible issue with
        // lifetime of information in the buffer if accessed beyond
        // the pipe line.
        return new XMLStreamBuffer();
    }


    /**
     * Creates a new {@link StreamSOAPDecoder} instance.
     */
    public static StreamSOAPDecoder create(SOAPVersion version) {
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
