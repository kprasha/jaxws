package com.sun.xml.ws.sandbox.message.impl.source;

import com.sun.xml.bind.marshaller.SAX2DOMEx;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.streaming.SourceReaderFactory;
import com.sun.xml.ws.util.xml.XmlUtil;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader;
import com.sun.xml.ws.sandbox.message.impl.Util;
import com.sun.xml.ws.sandbox.message.impl.RootElementSniffer;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.Attributes;
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
    // information about this header. lazily obtained.
    private String nsUri;
    private String localName;
    protected String role;

    /**
     * See the <tt>FLAG_***</tt> constants.
     */
    protected int flags;

    protected final SourceUtils sourceUtils;
    protected StreamHeader sh;

    public SourceHeader(Source src) {
        this.src = src;
        sourceUtils = new SourceUtils(src);

    }

    public XMLStreamReader readHeader() throws XMLStreamException {
        return SourceReaderFactory.createSourceReader(src, true);
    }

    public <T> T readAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        return (T)unmarshaller.unmarshal(src);
    }

    public void writeTo(XMLStreamWriter w) throws XMLStreamException {
        if(sourceUtils.isStreamSource()){
            sh.writeTo(w);
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

    protected final boolean isSet(int flagMask) {
        return (flags&flagMask)!=0;
    }

    protected final void set(int flagMask) {
        flags |= flagMask;
    }

    /**
     * Lazily parse the first element to obtain attribute values on it.
     */
    protected final void parseIfNecessary() {
        if(isSet(FLAG_PARSED))
            return;

        /**
         * if its StreamSource, its backed by StreamHeader implementation
         *  so let mark the FLAG_PARSED
         */

        if(sourceUtils.isStreamSource()){
            localName = sh.getLocalPart();
            nsUri = sh.getNamespaceURI();
            if(sh.isMustUnderstood())
                set(FLAG_MUST_UNDERSTAND);
            role = sh.getRole();            
            set(FLAG_PARSED);
            return;
        }

        //now its either a sax or dom Source
        RootElementSniffer sniffer = new RootElementSniffer();
        QName name = sourceUtils.sniff(src, sniffer);
        localName = name.getLocalPart();
        nsUri = name.getNamespaceURI();
        checkHeaderAttribute(sniffer.getAttributes());
        set(FLAG_PARSED);
    }

    /**
     * Checks for well-known SOAP attributes.
     *
     * Note that JAXB RI produces interned attribute names.
     */
    protected abstract void checkHeaderAttribute(Attributes a);

    protected final void checkMustUnderstand(String localName, Attributes a, int i) {
        if(localName=="mustUnderstand" && Util.parseBool(a.getValue(i)))
            set(FLAG_MUST_UNDERSTAND);
    }

    public final boolean isMustUnderstood() {
        parseIfNecessary();
        return isSet(FLAG_MUST_UNDERSTAND);
    }

    public String getRole() {
        parseIfNecessary();
        return role;
    }

    public String getNamespaceURI() {
        if(nsUri==null)
            parseIfNecessary();
        return nsUri;
    }

    public String getLocalPart() {
        if(localName==null)
            parseIfNecessary();
        return localName;
    }

    protected static final int FLAG_PARSED            = 0x0001;
    protected static final int FLAG_MUST_UNDERSTAND   = 0x0002;
    protected static final int FLAG_RELAY             = 0x0004;

    private static final LocatorImpl NULL_LOCATOR = new LocatorImpl();
}
