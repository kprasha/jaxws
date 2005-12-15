package com.sun.xml.ws.sandbox.message.impl.jaxb;

import com.sun.xml.ws.sandbox.message.HeaderList;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.message.MessageProperties;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferResult;
import com.sun.xml.stream.buffer.stax.StreamReaderBufferProcessor;
import org.xml.sax.Attributes;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.bind.util.JAXBResult;
import javax.xml.transform.Source;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

/**
 * {@link Message} backed by a JAXB bean.
 *
 * @author Kohsuke Kawaguchi
 */
public /*for now, work in progress*/ abstract class JAXBMessage extends Message {
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
     * If we have the infoset representation, this field is non-null.
     */
    private XMLStreamBuffer infoset;

    public JAXBMessage( Marshaller marshaller, Object jaxbObject ) {
        props = new MessageProperties();
        this.marshaller = marshaller;
        this.jaxbObject = jaxbObject;
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
    }

    public boolean hasHeaders() {
        return (headers == null) ? false : headers.size() > 0;
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
            nsUri = sniffer.nsUri;
            localName = sniffer.localName;
        }
    }

    public Source readPayloadAsSource() {
        try {
            return new JAXBSource(marshaller,jaxbObject);
        } catch (JAXBException e) {
            throw new AssertionError(e);    // impossible
        }
    }

    public <T> T readAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
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
            StreamReaderBufferProcessor r = new StreamReaderBufferProcessor();
            r.setXMLStreamBuffer(infoset);
            return r;
        } catch (JAXBException e) {
            throw new XMLStreamException(e);
        }
    }
}
