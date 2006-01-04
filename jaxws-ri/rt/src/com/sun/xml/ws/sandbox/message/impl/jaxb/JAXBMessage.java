/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.ws.sandbox.message.impl.jaxb;

import com.sun.xml.bind.marshaller.SAX2DOMEx;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferResult;
import com.sun.xml.ws.encoding.soap.SOAPVersion;
import com.sun.xml.ws.sandbox.message.HeaderList;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.message.MessageProperties;
import com.sun.xml.ws.sandbox.message.impl.AbstractMessageImpl;
import com.sun.xml.ws.util.xml.XmlUtil;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.util.JAXBResult;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;

/**
 * {@link Message} backed by a JAXB bean.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JAXBMessage extends AbstractMessageImpl {
    private HeaderList headers;
    private final MessageProperties props;

    /**
     * The JAXB object that represents the header.
     */
    private final Object jaxbObject;

    private final Marshaller marshaller;

    /**
     * Lazily sniffed payload element name
     */
    private String nsUri,localName;

    /**
     * If we have the infoset representation for the payload, this field is non-null.
     */
    private XMLStreamBuffer infoset;

    private final SOAPVersion soapVer;

    /**
     * Creates a {@link Message} backed by a JAXB bean.
     *
     * @param marshaller
     *      The marshaller to be used to produce infoset from the object. Must not be null.
     * @param jaxbObject
     *      The JAXB object that represents the payload. must not be null. This object
     *      must be bound to an element (which means it either is a {@link JAXBElement} or
     *      an instanceof a class with {@link XmlRootElement}).
     * @param soapVer
     *      The SOAP version of the message. Must not be null.
     */
    public JAXBMessage( Marshaller marshaller, Object jaxbObject, SOAPVersion soapVer ) {
        props = new MessageProperties();
        this.marshaller = marshaller;
        this.jaxbObject = jaxbObject;
        this.soapVer = soapVer;
    }

    /**
     * Creates a {@link Message} backed by a JAXB bean.
     *
     * @param marshaller
     *      The marshaller to be used to produce infoset from the object. Must not be null.
     * @param tagName
     *      The tag name of the payload element. Must not be null.
     * @param declaredType
     *      The expected type (IOW the declared defined in the schema) of the instance.
     *      If this is different from <tt>jaxbTypeObject</tt>, @xsi:type will be produced.
     * @param jaxbTypeObject
     *      The JAXB object that represents the payload. must not be null. This object
     *      may be a type that binds to an XML type, not an element, such as {@link String}
     *      or {@link Integer} that doesn't necessarily have an element name.
     * @param soapVer
     *      The SOAP version of the message. Must not be null.
     */
    public <T> JAXBMessage( Marshaller marshaller, QName tagName, Class<T> declaredType, T jaxbTypeObject, SOAPVersion soapVer ) {
        this(marshaller,new JAXBElement<T>(tagName,declaredType,jaxbTypeObject),soapVer);
        // fill in those known values eagerly
        this.nsUri = tagName.getNamespaceURI();
        this.localName = tagName.getLocalPart();
    }

    /**
     * Copy constructor.
     */
    public JAXBMessage(JAXBMessage that) {
        this.headers = that.headers;
        if(this.headers!=null)
            this.headers = new HeaderList(this.headers);
        // TODO: do we need to clone this? I guess so.
        this.props = that.props;

        this.jaxbObject = that.jaxbObject;
        // TODO: we need a different marshaller
        this.marshaller = that.marshaller;
        this.soapVer = that.soapVer;
    }

    public boolean hasHeaders() {
        return headers!=null && !headers.isEmpty();
    }

    public HeaderList getHeaders() {
        if(headers==null)
            headers = new HeaderList();
        return headers;
    }

    public MessageProperties getProperties() {
        return props;
    }

    public String getPayloadLocalPart() {
        if(localName==null)
            sniff();
        return localName;
    }

    public String getPayloadNamespaceURI() {
        if(nsUri==null)
            sniff();
        return nsUri;
    }

    /**
     * Obtains the tag name of the root element.
     */
    private void sniff() {
        RootElementSniffer sniffer = new RootElementSniffer();
        try {
            marshaller.marshal(jaxbObject,sniffer);
        } catch (JAXBException e) {
            // if it's due to us aborting the processing after the first element,
            // we can safely ignore this exception.
            //
            // if it's due to error in the object, the same error will be reported
            // when the readHeader() method is used, so we don't have to report
            // an error right now.
            nsUri = sniffer.getNsUri();
            localName = sniffer.getLocalName();
        }
    }

    public Source readPayloadAsSource() {
        try {
            return new JAXBSource(marshaller,jaxbObject);
        } catch (JAXBException e) {
            throw new AssertionError(e);    // impossible
        }
    }

    public <T> T readPayloadAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        JAXBResult out = new JAXBResult(unmarshaller);
        marshaller.marshal(jaxbObject,out);
        return (T)out.getResult();
    }

    public XMLStreamReader readPayload() throws XMLStreamException {
        try {
            if(infoset==null) {
                infoset = new XMLStreamBuffer();
                XMLStreamBufferResult sbr = new XMLStreamBufferResult(infoset);
                marshaller.marshal(jaxbObject,sbr);
            }
            return infoset.processUsingXMLStreamReader();
        } catch (JAXBException e) {
            throw new XMLStreamException(e);
        }
    }

    /**
     * Writes the whole envelope as SAX events.
     */
    public void writeTo( ContentHandler contentHandler, ErrorHandler errorHandler ) throws SAXException {
        String soapNsUri = soapVer.nsUri;

        contentHandler.setDocumentLocator(NULL_LOCATOR);
        contentHandler.startDocument();
        contentHandler.startPrefixMapping("S",soapNsUri);
        contentHandler.startElement(soapNsUri,"Envelope","S:Envelope",EMPTY_ATTS);
        contentHandler.startElement(soapNsUri,"Header","S:Header",EMPTY_ATTS);
        if(hasHeaders()) {
            int len = headers.size();
            for( int i=0; i<len; i++ ) {
                // shouldn't JDK be smart enough to use array-style indexing for this foreach!?
                headers.get(i).writeTo(contentHandler,errorHandler);
            }
        }
        contentHandler.endElement(soapNsUri,"Header","S:Header");
        // write the body
        contentHandler.startElement(soapNsUri,"Body","S:Body",EMPTY_ATTS);
        try {
            try {
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT,true);
                marshaller.marshal(jaxbObject,contentHandler);
            } finally {
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT,false);
            }
        } catch (JAXBException e) {
            errorHandler.fatalError(new SAXParseException(e.getMessage(),NULL_LOCATOR,e));
        }
        contentHandler.endElement(soapNsUri,"Body","S:Body");
        contentHandler.endElement(soapNsUri,"Envelope","S:Envelope");
    }

    public void writeTo(XMLStreamWriter w) throws XMLStreamException {
        String soapNsUri = soapVer.nsUri;
        w.writeStartDocument();
        w.writeNamespace("S",soapNsUri);
        w.writeStartElement("S","Envelope",soapNsUri);
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
        try {
            try {
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT,true);
                marshaller.marshal(jaxbObject,w);
            } finally {
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT,false);
            }
        } catch (JAXBException e) {
            throw new XMLStreamException(e);
        }
        w.writeEndElement();
        w.writeEndElement();
        w.writeEndDocument();
    }

    public SOAPMessage readAsSOAPMessage() throws SOAPException {
        SOAPMessage msg = soapVer.saajFactory.createMessage();
        SAX2DOMEx s2d = new SAX2DOMEx(msg.getSOAPPart());
        try {
            writeTo(s2d, XmlUtil.DRACONIAN_ERROR_HANDLER);
        } catch (SAXException e) {
            throw new SOAPException(e);
        }
        // TODO: add attachments and so on.
        // we can use helper classes, I think.
        return msg;
    }

    public void writePayloadTo(XMLStreamWriter sw) throws XMLStreamException {
        try {
            // TODO: XOP handling
            marshaller.marshal(jaxbObject,sw);
        } catch (JAXBException e) {
            throw new XMLStreamException(e);
        }
    }

    public Message copy() {
        return new JAXBMessage(this);
    }


    private static final Attributes EMPTY_ATTS = new AttributesImpl();
    private static final LocatorImpl NULL_LOCATOR = new LocatorImpl();
}
