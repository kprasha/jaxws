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
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.streaming.SourceReaderFactory;
import com.sun.xml.ws.util.DOMUtil;
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
 * @author Vivek Pandey
 */

public class SAAJHeader implements Header{

    private SOAPHeaderElement header;
    private boolean isMustUnderstood;
    private String role;
    private boolean relay;
    private int flags;
    private String localName;
    private String namespaceUri;

    protected static final int FLAG_ACTOR            = 0x0001;
    protected static final int FLAG_MUST_UNDERSTAND   = 0x0002;
    protected static final int FLAG_RELAY             = 0x0004;


    public SAAJHeader(SOAPHeaderElement header) {
        this.header = header;
        localName = header.getLocalName();
        namespaceUri = header.getNamespaceURI();
    }

    /**
     * True if this header must be understood.
     *
     * Read the mustUndestandHeader only once, save reading it from DOM everytime.
     */
    public boolean isMustUnderstood() {
        if(isSet(FLAG_MUST_UNDERSTAND))
            return isMustUnderstood;

        isMustUnderstood = header.getMustUnderstand();
        set(FLAG_MUST_UNDERSTAND);
        return isMustUnderstood;
    }

    /**
     * Gets the value of the soap:role attribute (or soap:actor for SOAP 1.1).
     * <p/>
     * <p/>
     * SOAP 1.1 values are normalized into SOAP 1.2 values.
     * <p/>
     * An omitted SOAP 1.1 actor attribute value will become:
     * "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver"
     * An SOAP 1.1 actor attribute value of:
     * "http://schemas.xmlsoap.org/soap/actor/next"
     * will become:
     * "http://www.w3.org/2003/05/soap-envelope/role/next"
     * <p/>
     * <p/>
     * If the soap:role attribute is absent, this method returns
     * "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver".
     *
     * @return never null. This string need not be interned.
     */
    public String getRole() {
        if(isSet(FLAG_ACTOR))
            return role;

        role = header.getActor();

        //SAAJ may return null, lets return the default value in that case
        //TODO: findout SOAP version
        if(role == null)
            role = "http://schemas.xmlsoap.org/soap/actor/next";

        set(FLAG_ACTOR);
        return role;
    }

    /**
     * True if this header is to be relayed if not processed.
     * For SOAP 1.1 messages, this method always return false.
     * <p/>
     * <p/>
     * IOW, this method returns true if there's @soap:relay='true'
     * is present.
     * <p/>
     * <h3>Implementation Note</h3>
     * <p/>
     * The implementation needs to check for both "true" and "1",
     * but because attribute values are normalized, it doesn't have
     * to consider " true", " 1 ", and so on.
     *
     * @return false.
     */
    public boolean isRelay() {
        if(isSet(FLAG_RELAY))
            return relay;

        //SAAJ throws UnsupportedOperationException if its SOAP 1.1 version
        //Ideally this method should always throw false for SOAP 1.1
        try{
            relay = header.getRelay();
        }catch(UnsupportedOperationException e){
            relay = false;
        }
        return relay;
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
     * For some {@link com.sun.xml.ws.api.message.Header} implementations, this operation
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

    protected boolean isSet(int flag){
        return (flags&flag) != 0;
    }

    protected void set(int flag){
        flags |= flag;
    }
}
