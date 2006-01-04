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
package com.sun.xml.ws.sandbox.message.impl.saaj;

import com.sun.xml.bind.unmarshaller.DOMScanner;
import com.sun.xml.ws.sandbox.message.Header;
import com.sun.xml.ws.sandbox.message.impl.AbstractHeaderImpl;
import com.sun.xml.ws.streaming.SourceReaderFactory;
import com.sun.xml.ws.util.DOMUtil;
import com.sun.xml.ws.encoding.soap.SOAPVersion;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.WebServiceException;

/**
 * {@link Header} backed by a {@link SOAPHeaderElement}.
 *
 * @author Vivek Pandey
 */
public final class SAAJHeader extends AbstractHeaderImpl {

    private SOAPHeaderElement header;
    private String localName;
    private String namespaceUri;



    public SAAJHeader(SOAPHeaderElement header, SOAPVersion soapVersion) {
        super(soapVersion);
        this.header = header;
        localName = header.getLocalName();
        namespaceUri = header.getNamespaceURI();
    }

    public String getAttribute(String nsUri, String localName) {
        return header.getAttributeNS(nsUri,localName);
    }

    /**
     * Gets the namespace URI of this header element.
     *
     * @return never null.
     *         this string must be interned.
     */
    public String getNamespaceURI() {
        return namespaceUri;
    }

    /**
     * Gets the local name of this header element.
     *
     * @return never null.
     *         this string must be interned.
     */
    public String getLocalPart() {
        return localName;
    }

    /**
     * Reads the header as a {@link javax.xml.stream.XMLStreamReader}.
     * <p/>
     * <p/>
     * <h3>Performance Expectation</h3>
     * <p/>
     * For some {@link com.sun.xml.ws.sandbox.message.Header} implementations, this operation
     * is a non-trivial operation. Therefore, use of this method
     * is discouraged unless the caller is interested in reading
     * the whole header.
     * <p/>
     * <p/>
     * Similarly, if the caller wants to use this method only to do
     * the API conversion (such as simply firing SAX events from
     * {@link javax.xml.stream.XMLStreamReader}), then the JAX-WS team requests
     * that you talk to us.
     *
     * @return must not null.
     */
    public XMLStreamReader readHeader() throws XMLStreamException {
        return SourceReaderFactory.createSourceReader(new DOMSource(header), true);
    }

    /**
     * Reads the header as a JAXB object by using the given unmarshaller.
     */
    public <T> T readAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        try {
            return (T) unmarshaller.unmarshal(header);
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Writes out the header.
     *
     * @throws javax.xml.stream.XMLStreamException
     *          if the operation fails for some reason. This leaves the
     *          writer to an undefined state.
     */
    public void writeTo(XMLStreamWriter w) throws XMLStreamException {
        try {
            DOMUtil.serializeNode(header, w);
        } catch (XMLStreamException e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Writes out the header to the given SOAPMessage.
     *
     * When will we need this method?
     *
     * @throws javax.xml.soap.SOAPException if the operation fails for some reason. This leaves the
     *                                      writer to an undefined state.
     */
    public void writeTo(SOAPMessage saaj) throws SOAPException {
        // TODO: check if this is really fast enough
        SOAPHeader header = saaj.getSOAPHeader();
        Node node = header.getOwnerDocument().importNode(this.header, true);
        header.appendChild(node);
    }

    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        DOMScanner ds = new DOMScanner();
        ds.setContentHandler(contentHandler);
        ds.scan(header);
    }
}
