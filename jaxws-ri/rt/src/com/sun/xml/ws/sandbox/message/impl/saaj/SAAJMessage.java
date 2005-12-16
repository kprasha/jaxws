package com.sun.xml.ws.sandbox.message.impl.saaj;

import com.sun.xml.ws.encoding.soap.SOAPConstants;
import com.sun.xml.ws.sandbox.XMLStreamWriterEx;
import com.sun.xml.ws.sandbox.message.Attachment;
import com.sun.xml.ws.sandbox.message.AttachmentSet;
import com.sun.xml.ws.sandbox.message.HeaderList;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.message.MessageProperties;
import com.sun.xml.ws.sandbox.message.Header;
import com.sun.xml.ws.streaming.SourceReaderFactory;
import com.sun.xml.ws.util.SOAPUtil;
import com.sun.xml.ws.util.DOMUtil;
import com.sun.xml.ws.util.xml.XmlUtil;
import org.w3c.dom.Node;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Vivek Pandey
 */

public class SAAJMessage extends Message{
    private SOAPMessage sm;
    private HeaderList headers;
    private MessageProperties properties;
    private String payloadLocalName;
    private String payloadNamspace;
    private SAAJAttachmentSet attSet;

    private boolean parsedHeader;

    public SAAJMessage(SOAPMessage sm){
        properties = new MessageProperties();

        this.sm = sm;
        try {
            Node body = sm.getSOAPBody();
            Node n = body.getFirstChild();
            // hope this is correct. Caching the localname and namespace of the payload should be fine
            // but what about if a Handler replaces the payload with something else? Weel, may be it
            // will be error condition anyway
            if(n != null){
                payloadLocalName =  n.getLocalName();
                payloadNamspace = n.getNamespaceURI();
            }
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }

    public SAAJMessage(HeaderList headers, SOAPMessage sm) {
        this(sm);
        this.headers = headers;
    }

    public boolean hasHeaders() {
        return getHeaders().size() > 0;
    }

    /**
     * Gets all the headers of this message.
     * @return always return the same non-null object.
     */
    public HeaderList getHeaders() {
        if(parsedHeader)
            return headers;

        if(headers == null)
            headers = new HeaderList();

        try {
            SOAPHeader header = sm.getSOAPHeader();
            Iterator iter = header.examineAllHeaderElements();
            while(iter.hasNext()){
                headers.add(new SAAJHeader((SOAPHeaderElement)iter.next()));
            }
            parsedHeader = true;
        } catch (SOAPException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return headers;
    }

    /**
     * Returns the properties of this message.
     *
     * @return always return the same object. Never null.
     */
    public MessageProperties getProperties() {
        return properties;
    }

    /**
     * Gets the attachments of this message
     * (attachments live outside a message.)
     */
    public AttachmentSet getAttachments() {
        if(attSet == null)
            attSet =  new SAAJAttachmentSet(sm);
        return attSet;
    }

    /**
     * Gets the local name of the payload element.
     *
     */
    public String getPayloadLocalPart() {
        return payloadLocalName;
    }

    /**
     * Gets the namespace URI of the payload element.
     */
    public String getPayloadNamespaceURI() {
        return payloadNamspace;
    }

    /**
     * Consumes this message including the envelope.
     * returns it as a {@link javax.xml.transform.Source} object.
     */
    public Source readEnvelopeAsSource() {
        try {
            SOAPEnvelope se = sm .getSOAPPart().getEnvelope();
            return new DOMSource(se);
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Returns the payload as a {@link javax.xml.transform.Source} object.
     * <p/>
     * This consumes the message.
     */
    public Source readPayloadAsSource() {
        try {
            Node pn = sm.getSOAPBody().getFirstChild();
            return (pn != null)?new DOMSource(pn):null;
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }

    }

    /**
     * Creates the equivalent {@link javax.xml.soap.SOAPMessage} from this message.
     * <p/>
     * This consumes the message.
     */
    public SOAPMessage readAsSOAPMessage() {
        return sm;
    }

    /**
     * Reads the payload as a JAXB object by using the given unmarshaller.
     * <p/>
     * This consumes the message.
     */
    public <T> T readAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        try {
            Node pn = sm.getSOAPBody().getFirstChild();
            if(pn != null)
                return (T) unmarshaller.unmarshal(pn);
            return null;
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Reads the payload as a {@link javax.xml.stream.XMLStreamReader}
     * <p/>
     * This consumes the message.
     */
    public XMLStreamReader readPayload() {
        try {
            Node pn = sm.getSOAPBody().getFirstChild();
            return (pn != null)?
            SourceReaderFactory.createSourceReader(new DOMSource(pn), true):null;
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Writes the payload to StAX.
     * <p/>
     * This method writes just the payload of the message to the writer.
     * This consumes the message.
     */
    public void writePayloadTo(XMLStreamWriter sw) {
        try {
            Node pn = sm.getSOAPBody().getFirstChild();
            if(pn != null)
                DOMUtil.serializeNode(pn,sw);
        } catch (XMLStreamException e) {
            throw new WebServiceException(e);
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Writes the whole SOAP message (but not attachments)
     * to the given writer.
     * <p/>
     * This consumes the message.
     */
    public void writeTo(XMLStreamWriter sw) {
        try {
            SOAPEnvelope se = sm.getSOAPPart().getEnvelope();
            DOMUtil.serializeNode(se,sw);
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        } catch (XMLStreamException e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * Creates a copy of a {@link com.sun.xml.ws.sandbox.message.Message}.
     * <p/>
     * <p/>
     * This method creates a new {@link com.sun.xml.ws.sandbox.message.Message} whose header/payload/attachments/properties
     * are identical to this {@link com.sun.xml.ws.sandbox.message.Message}. Once created, the created {@link com.sun.xml.ws.sandbox.message.Message}
     * and the original {@link com.sun.xml.ws.sandbox.message.Message} behaves independently --- adding header/
     * attachment to one {@link com.sun.xml.ws.sandbox.message.Message} doesn't affect another {@link com.sun.xml.ws.sandbox.message.Message}
     * at all.
     * <p/>
     * <h3>Design Rationale</h3>
     * <p/>
     * Since a {@link com.sun.xml.ws.sandbox.message.Message} body is read-once, sometimes
     * (such as when you do fail-over, or WS-RM) you need to
     * create an idential copy of a {@link com.sun.xml.ws.sandbox.message.Message}.
     * <p/>
     * <p/>
     * The actual copy operation depends on the layout
     * of the data in memory, hence it's best to be done by
     * the {@link com.sun.xml.ws.sandbox.message.Message} implementation itself.
     */
    public Message copy() {
        try {
            SOAPEnvelope se = sm.getSOAPPart().getEnvelope();
            SOAPMessage msg = null;
            if(se.getNamespaceURI().equals(SOAPConstants.QNAME_SOAP_ENVELOPE.getNamespaceURI())){
                msg = SOAPUtil.createMessage(SOAPBinding.SOAP11HTTP_BINDING);
            }else{
                msg = SOAPUtil.createMessage(SOAPBinding.SOAP12HTTP_BINDING);
            }
            msg.getSOAPPart().getEnvelope().getOwnerDocument().importNode(se, true);
            Iterator iter = sm.getAttachments();
            while(iter.hasNext()){
                msg.addAttachmentPart((AttachmentPart)iter.next());
            }
            return new SAAJMessage(getHeaders(), msg);
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }




    private class SAAJAttachment implements Attachment{

        AttachmentPart ap;

        public SAAJAttachment(AttachmentPart part) {
            this.ap = part;
        }

        /**
         * Content ID of the attachment. Uniquely identifies an attachment.
         */
        public String getContentId() {
            return ap.getContentId();
        }

        /**
         * Gets the MIME content-type of this attachment.
         */
        public String getContentType() {
            return ap.getContentType();
        }

        /**
         * Gets the attachment as an exact-length byte array.
         */
        public byte[] asByteArray() {
            try {
                return ap.getRawContentBytes();
            } catch (SOAPException e) {
                throw new WebServiceException(e);
            }
        }

        /**
         * Gets the attachment as a {@link javax.activation.DataHandler}.
         */
        public DataHandler asDataHandler() {
            try {
                return ap.getDataHandler();
            } catch (SOAPException e) {
                throw new WebServiceException(e);
            }
        }

        /**
         * Gets the attachment as a {@link javax.xml.transform.Source}.
         * Note that there's no guarantee that the attachment is actually an XML.
         */
        public Source asSource() {
            try {
                return new StreamSource(ap.getRawContent());
            } catch (SOAPException e) {
                throw new WebServiceException(e);
            }
        }

        /**
         * Obtains this attachment as an {@link java.io.InputStream}.
         */
        public InputStream asInputStream() {
            try {
                return ap.getRawContent();
            } catch (SOAPException e) {
                throw new WebServiceException(e);
            }
        }

        /**
         * Writes the contents of the attachment into the given stream.
         */
        public void writeTo(OutputStream os) throws IOException {
            os.write(asByteArray());
        }

        /**
         * Writes this attachment to the given {@link javax.xml.soap.SOAPMessage}.
         */
        public void writeTo(SOAPMessage saaj) {
            saaj.addAttachmentPart(ap);
        }
    }

    private class SAAJAttachmentSet implements AttachmentSet {

        private SOAPMessage sm;
        private Map<String, Attachment> attMap;
        private Iterator attIter;

        public SAAJAttachmentSet(SOAPMessage sm) {
            this.sm = sm;
            attIter = sm.getAttachments();
        }

        /**
         * Gets the attachment by the content ID.
         *
         * @return null
         *         if no such attachment exist.
         */
        public Attachment get(String contentId) {
            if(!attIter.hasNext())
                return null;

            // if this is the first time then create the attachment Map
            if(attMap == null){
                attMap = createAttachmentMap();
            }
            return attMap.get(contentId);
        }

        /**
         * Returns an iterator over a set of elements of type T.
         *
         * @return an Iterator.
         */
        public Iterator<Attachment> iterator() {
            if(attMap == null){
                attMap = createAttachmentMap();
            }
            return attMap.values().iterator();
        }

        private Map<String, Attachment> createAttachmentMap(){
            HashMap<String, Attachment> map = new HashMap<String, Attachment>();
            while(attIter.hasNext()){
                AttachmentPart ap = (AttachmentPart)attIter.next();
                attMap.put(ap.getContentId(), new SAAJAttachment(ap));
            }
            return map;
        }
    }
}
