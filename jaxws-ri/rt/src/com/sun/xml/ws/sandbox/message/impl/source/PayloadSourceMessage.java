package com.sun.xml.ws.sandbox.message.impl.source;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.bind.marshaller.SAX2DOMEx;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.sandbox.message.impl.AbstractMessageImpl;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamMessage;
import com.sun.xml.ws.streaming.SourceReaderFactory;
import com.sun.xml.ws.streaming.XMLStreamReaderFactory;
import com.sun.xml.ws.streaming.XMLStreamReaderUtil;
import com.sun.xml.ws.util.ASCIIUtility;
import com.sun.xml.ws.util.xml.XmlUtil;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferSource;
import com.sun.xml.stream.buffer.sax.SAXBufferCreator;
import com.sun.xml.stream.buffer.sax.SAXBufferProcessor;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.WebServiceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;

/**
 * Payloadsource message that can be constructed for the StreamSource, SAXSource and DOMSource.
 *
 * @author Vivek Pandey
 *
 */
public class PayloadSourceMessage extends AbstractMessageImpl {
    private Source src;
    private String localName;
    private String namespaceUri;
    private HeaderList headers;
    private SourceUtils sourceUtils;

    private Message streamMessage;
    private byte[] payloadbytes;

    /**
     * Gets the {@link Message} based on {@link Source} representing payload.
     *
     * @param headers may be null
     * @param src must be non-null
     * @param soapVersion  must be non-null, posible values are {@link SOAPVersion#SOAP_11} or {@link SOAPVersion#SOAP_12}
     */
    public PayloadSourceMessage(HeaderList headers, Source src, SOAPVersion soapVersion) {
        super(soapVersion);
        this.headers = headers;
        this.src = src;
        sourceUtils = new SourceUtils(src);
        if(src instanceof StreamSource){
            StreamSource streamSource = (StreamSource)src;
            try {
                payloadbytes = ASCIIUtility.getBytes(streamSource.getInputStream());
            } catch (IOException e) {
                throw new WebServiceException(e);
            }
            streamSource.setInputStream(new ByteArrayInputStream(payloadbytes));
            XMLStreamReader reader = XMLStreamReaderFactory.createXMLStreamReader(streamSource.getInputStream(), true);
            //move the cursor to the payload element
            XMLStreamReaderUtil.next(reader);
            streamMessage = new StreamMessage(headers, reader, soapVersion);
        }
    }

    public PayloadSourceMessage(Source s, SOAPVersion soapVer) {
        this(null, s, soapVer);
    }

    public boolean hasHeaders() {
        if(headers == null)
            return false;
        return headers.size() > 0;
    }

    public HeaderList getHeaders() {
        if(headers == null)
            headers = new HeaderList();
        return headers;
    }

    public String getPayloadLocalPart() {
        if(localName != null)
            return localName;

        if(sourceUtils.isStreamSource()){
            localName = streamMessage.getPayloadLocalPart();
        }else{
            QName name = sourceUtils.sniff(src);
            localName = name.getLocalPart();
            namespaceUri = name.getNamespaceURI();
        }
        return localName;
    }

    public String getPayloadNamespaceURI() {
        if(namespaceUri != null)
            return namespaceUri;

        if(sourceUtils.isStreamSource()){
            namespaceUri = streamMessage.getPayloadNamespaceURI();
        }else{
            QName name = sourceUtils.sniff(src);
            localName = name.getLocalPart();
            namespaceUri = name.getNamespaceURI();
        }
        return namespaceUri;
    }

    public boolean hasPayload() {
        return true;
    }


    public Source readPayloadAsSource() {
        return src;
    }

    protected void writePayloadTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException{
        SAXResult sr = new SAXResult(contentHandler);
        try {
            Transformer transformer = XmlUtil.newTransformer();
            transformer.transform(src, sr);
        } catch (TransformerConfigurationException e) {
            errorHandler.fatalError(new SAXParseException(e.getMessage(),NULL_LOCATOR,e));
        } catch (TransformerException e) {
            errorHandler.fatalError(new SAXParseException(e.getMessage(),NULL_LOCATOR,e));
        }
    }

    public Object readPayloadAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        if(sourceUtils.isStreamSource())
            return streamMessage.readPayloadAsJAXB(unmarshaller);
        return unmarshaller.unmarshal(src);
    }

    public <T> T readPayloadAsJAXB(Bridge<T> bridge, BridgeContext context) throws JAXBException {
        if(sourceUtils.isStreamSource())
            return streamMessage.readPayloadAsJAXB(bridge,context);
        return bridge.unmarshal(context,src);
    }

    public XMLStreamReader readPayload() throws XMLStreamException {
        if(sourceUtils.isStreamSource()){
            return streamMessage.readPayload();
        }
        XMLStreamReader reader =  SourceReaderFactory.createSourceReader(src, true);
        //position the reader at start tag then return
        XMLStreamReaderUtil.next(reader);
        return reader;
    }

    public void writePayloadTo(XMLStreamWriter w) throws XMLStreamException {
        if(sourceUtils.isStreamSource()){
            streamMessage.writePayloadTo(w);
            return;
        }
        SourceUtils.serializeSource(src, w);
    }

    public void writeTo(XMLStreamWriter w) throws XMLStreamException {
        if(sourceUtils.isStreamSource()){
            streamMessage.writeTo(w);
            w.flush();
            w.close();
            return;
        }
        String soapNsUri = soapVersion.nsUri;
        w.writeStartDocument();
       // w.writeNamespace("S",soapNsUri);
        w.writeStartElement("S","Envelope",soapNsUri);
        w.writeNamespace("S",soapNsUri);

        //write soapenv:Header
        w.writeStartElement("S","Header",soapNsUri);
        if(hasHeaders()) {
            int len = headers.size();
            for( int i=0; i<len; i++ ) {
                headers.get(i).writeTo(w);
            }
        }
        w.writeEndElement();

        // write the body
        w.writeStartElement("S","Body",soapNsUri);
        SourceUtils.serializeSource(src, w);
        w.writeEndElement();

        w.writeEndElement();
        w.writeEndDocument();
        w.flush();
    }

    public Message copy() {
        Message msg = null;
        if(sourceUtils.isStreamSource()){
            StreamSource ss = (StreamSource)src;
            ByteArrayInputStream bis = new ByteArrayInputStream(payloadbytes);
            StreamSource newSource = new StreamSource(bis, src.getSystemId());
            newSource.setReader(ss.getReader());
            return new PayloadSourceMessage(HeaderList.copy(headers), newSource, soapVersion);
        }else if(sourceUtils.isSaxSource()){
            SAXSource saxSrc = (SAXSource)src;
            try {
                XMLStreamBuffer xsb = new XMLStreamBuffer();
                XMLReader reader = saxSrc.getXMLReader();
                if(reader == null)
                    reader = new SAXBufferProcessor();
                saxSrc.setXMLReader(reader);
                reader.setContentHandler(new SAXBufferCreator(xsb));
                reader.parse(saxSrc.getInputSource());
                src = new XMLStreamBufferSource(xsb);
                return new PayloadSourceMessage(HeaderList.copy(headers),
                        new XMLStreamBufferSource(xsb), soapVersion);
            } catch (IOException e) {
                throw new WebServiceException(e);
            } catch (SAXException e) {
                throw new WebServiceException(e);
            }
        }else if(sourceUtils.isDOMSource()){
            DOMSource ds = (DOMSource)src;
            try {
                SAX2DOMEx s2d = new SAX2DOMEx();
                writePayloadTo(s2d, XmlUtil.DRACONIAN_ERROR_HANDLER);
                Source newDomSrc = new DOMSource(s2d.getDOM(), ds.getSystemId());
                msg = new PayloadSourceMessage(HeaderList.copy(headers), newDomSrc, soapVersion);
            } catch (ParserConfigurationException e) {
                throw new WebServiceException(e);
            } catch (SAXException e) {
                throw new WebServiceException(e);
            }
        }
        return msg;
    }

    private static final LocatorImpl NULL_LOCATOR = new LocatorImpl();
}
