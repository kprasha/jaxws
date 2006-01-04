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

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferResult;
import com.sun.xml.ws.encoding.soap.SOAPVersion;
import com.sun.xml.ws.sandbox.message.Header;
import com.sun.xml.ws.sandbox.message.impl.AbstractHeaderImpl;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBResult;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * {@link Header} whose physical data representation is a JAXB bean.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JAXBHeader extends AbstractHeaderImpl {

    /**
     * The JAXB object that represents the header.
     */
    private final Object jaxbObject;

    private final Bridge bridge;
    private final BridgeContext context;

    // information about this header. lazily obtained.
    private String nsUri;
    private String localName;
    /**
     * Attributes on the header.
     */
    private Attributes attributes;

    /**
     * Once the header is turned into infoset,
     * this buffer keeps it.
     */
    private XMLStreamBuffer infoset;

    public JAXBHeader(Marshaller marshaller, Object jaxbObject, SOAPVersion soapver) {
        super(soapver);
        this.jaxbObject = jaxbObject;
        this.bridge = MarshallerBridgeContext.MARSHALLER_BRIDGE;
        this.context = new MarshallerBridgeContext(marshaller);

        if (jaxbObject instanceof JAXBElement) {
            JAXBElement e = (JAXBElement) jaxbObject;
            this.nsUri = e.getName().getNamespaceURI();
            this.localName = e.getName().getLocalPart();
        }
    }

    public JAXBHeader(Bridge bridge, BridgeContext bridgeInfo, Object jaxbObject, SOAPVersion soapver) {
        super(soapver);
        this.jaxbObject = jaxbObject;
        this.bridge = bridge;
        this.context = bridgeInfo;

        QName tagName = bridge.getTypeReference().tagName;
        this.nsUri = tagName.getNamespaceURI();
        this.localName = tagName.getLocalPart();
    }

    /**
     * Lazily parse the first element to obtain attribute values on it.
     */
    private void parse() {
        RootElementSniffer sniffer = new RootElementSniffer();
        try {
            bridge.marshal(context,jaxbObject,sniffer);
        } catch (JAXBException e) {
            // if it's due to us aborting the processing after the first element,
            // we can safely ignore this exception.
            //
            // if it's due to error in the object, the same error will be reported
            // when the readHeader() method is used, so we don't have to report
            // an error right now.
            nsUri = sniffer.getNsUri();
            localName = sniffer.getLocalName();
            attributes = sniffer.getAttributes();
        }
    }

    public String getNamespaceURI() {
        if(nsUri==null)
            parse();
        return nsUri;
    }

    public String getLocalPart() {
        if(localName==null)
            parse();
        return localName;
    }

    public String getAttribute(String nsUri, String localName) {
        if(attributes==null)
            parse();
        return attributes.getValue(nsUri,localName);
    }

    public XMLStreamReader readHeader() throws XMLStreamException {
        try {
            if(infoset==null) {
                infoset = new XMLStreamBuffer();
                XMLStreamBufferResult sbr = new XMLStreamBufferResult(infoset);
                bridge.marshal(context,jaxbObject,sbr);
            }
            return infoset.processUsingXMLStreamReader();
        } catch (JAXBException e) {
            throw new XMLStreamException(e);
        }
    }

    public <T> T readAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        JAXBResult r = new JAXBResult(unmarshaller);
        bridge.marshal(context,jaxbObject,r);
        return (T)r.getResult();
    }

    public void writeTo(XMLStreamWriter w) throws XMLStreamException {
        try {
            bridge.marshal(context,jaxbObject,w);
        } catch (JAXBException e) {
            throw new XMLStreamException(e);
        }
    }

    public void writeTo(SOAPMessage saaj) throws SOAPException {
        try {
            bridge.marshal(context,jaxbObject,saaj.getSOAPHeader());
        } catch (JAXBException e) {
            throw new SOAPException(e);
        }
    }

    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        try {
            bridge.marshal(context,jaxbObject,contentHandler);
        } catch (JAXBException e) {
            SAXParseException x = new SAXParseException(e.getMessage(),null,null,-1,-1,e);
            errorHandler.fatalError(x);
            throw x;
        }
    }

}
