package com.sun.xml.ws.sandbox.message.impl.source;

import com.sun.xml.bind.marshaller.SAX2DOMEx;
import com.sun.xml.ws.sandbox.message.Header;
import com.sun.xml.ws.streaming.SourceReaderFactory;
import com.sun.xml.ws.util.xml.XmlUtil;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.LocatorImpl;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;

/**
 * @author Vivek Pandey
 */
abstract class SourceHeader implements Header {

    Source src;
    String localName;
    String nsUri;
    SourceUtils sourceUtils;    

    public SourceHeader(Source src) {
        this.src = src;
        sourceUtils = new SourceUtils(src);
    }

    protected abstract Header getStreamHeader();

    public String getNamespaceURI() {
        if(nsUri != null)
            return nsUri;
        if(sourceUtils.isStreamSource()){
            nsUri = getStreamHeader().getNamespaceURI();
        }else{
            QName name = sourceUtils.sniff(src);
            localName = name.getLocalPart();
            nsUri = name.getNamespaceURI();
        }
        return nsUri;
    }

    public String getLocalPart() {
        if(localName != null)
            return localName;

        if(sourceUtils.isStreamSource()){
            localName = getStreamHeader().getLocalPart();
        }else{
            QName name = sourceUtils.sniff(src);
            localName = name.getLocalPart();
            nsUri = name.getNamespaceURI();
        }
        return localName;
    }

    public XMLStreamReader readHeader() throws XMLStreamException {
        return SourceReaderFactory.createSourceReader(src, true);
    }

    public <T> T readAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        return (T)unmarshaller.unmarshal(src);
    }

    public void writeTo(XMLStreamWriter w) throws XMLStreamException {
        if(sourceUtils.isStreamSource()){
            getStreamHeader().writeTo(w);
            return;
        }
        SourceUtils.serializeSource(src, w);
    }

    public void writeTo(SOAPMessage saaj) throws SOAPException {
        SAX2DOMEx s2d = new SAX2DOMEx(saaj.getSOAPHeader());
        try {
            writeTo(s2d, XmlUtil.DRACONIAN_ERROR_HANDLER);
        } catch (SAXException e) {
            throw new SOAPException(e);
        }
    }

    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
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

    private static final LocatorImpl NULL_LOCATOR = new LocatorImpl();
}
