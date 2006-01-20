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

import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferResult;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.sandbox.message.impl.Util;
import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
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
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.namespace.QName;

/**
 * {@link Header} whose physical data representation is a JAXB bean.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class JAXBHeader implements Header {

    /**
     * The JAXB object that represents the header.
     */
    private final Object jaxbObject;

    private final Bridge bridge;
    private final BridgeContext context;

    // information about this header. lazily obtained.
    private String nsUri;
    private String localName;
    protected String role;

    /**
     * See the <tt>FLAG_***</tt> constants.
     */
    protected int flags;

    /**
     * Once the header is turned into infoset,
     * this buffer keeps it.
     */
    private XMLStreamBuffer infoset;

    public JAXBHeader(Marshaller marshaller, Object jaxbObject) {
        this.jaxbObject = jaxbObject;
        this.bridge = MarshallerBridgeContext.MARSHALLER_BRIDGE;
        this.context = new MarshallerBridgeContext(marshaller);

        if (jaxbObject instanceof JAXBElement) {
            JAXBElement e = (JAXBElement) jaxbObject;
            this.nsUri = e.getName().getNamespaceURI();
            this.localName = e.getName().getLocalPart();
        }
    }

    public JAXBHeader(Bridge bridge, BridgeContext bridgeInfo, Object jaxbObject) {
        this.jaxbObject = jaxbObject;
        this.bridge = bridge;
        this.context = bridgeInfo;

        QName tagName = bridge.getTypeReference().tagName;
        this.nsUri = tagName.getNamespaceURI();
        this.localName = tagName.getLocalPart();
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

        RootElementSniffer sniffer = new RootElementSniffer() {
            @Override
            protected void checkAttributes(Attributes a) {
                JAXBHeader.this.checkHeaderAttribute(a);
            }
        };
        try {
            bridge.marshal(context,jaxbObject,sniffer);
        } catch (JAXBException e) {
            // if it's due to us aborting the processing after the first element,
            // we can safely ignore this exception.
            //
            // if it's due to error in the object, the same error will be reported
            // when the readHeader() method is used, so we don't have to report
            // an error right now.
            nsUri = sniffer.nsUri;
            localName = sniffer.localName;
        }
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

    public <T> T readAsJAXB(Bridge<T> bridge, BridgeContext context) throws JAXBException {
        return bridge.unmarshal(context,new JAXBBridgeSource(this.bridge,this.context,jaxbObject));
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

    protected static final int FLAG_PARSED            = 0x0001;
    protected static final int FLAG_MUST_UNDERSTAND   = 0x0002;
    protected static final int FLAG_RELAY             = 0x0004;

}
