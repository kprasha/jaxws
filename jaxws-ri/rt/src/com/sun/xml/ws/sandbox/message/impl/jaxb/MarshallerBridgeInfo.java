package com.sun.xml.ws.sandbox.message.impl.jaxb;

import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.TypeReference;

import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import java.io.OutputStream;
import java.io.InputStream;

import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

/**
 * Used to adapt {@link Marshaller} into a {@link BridgeContext}.
 *
 * Always used in a pair with {@link #MARSHALLER_BRIDGE}.
 *
 * @author Kohsuke Kawaguchi
 */
final class MarshallerBridgeContext extends BridgeContext {
    /**
     * Wrapped {@link Marshaller}.
     */
    private final Marshaller m;

    public MarshallerBridgeContext(Marshaller m) {
        this.m = m;
    }

    public void setErrorHandler(ValidationEventHandler handler) {
        try {
            m.setEventHandler(handler);
        } catch (JAXBException e) {
            throw new AssertionError(e);    // impossible
        }
    }

    public void setAttachmentMarshaller(AttachmentMarshaller am) {
        m.setAttachmentMarshaller(am);
    }

    public void setAttachmentUnmarshaller(AttachmentUnmarshaller m) {
        throw new UnsupportedOperationException();
    }

    public AttachmentMarshaller getAttachmentMarshaller() {
        return m.getAttachmentMarshaller();
    }

    public AttachmentUnmarshaller getAttachmentUnmarshaller() {
        throw new UnsupportedOperationException();
    }

    static final Bridge MARSHALLER_BRIDGE = new Bridge() {
        public void marshal(BridgeContext context, Object object, XMLStreamWriter output) throws JAXBException {
            ((MarshallerBridgeContext)context).m.marshal(object,output);
        }

        public void marshal(BridgeContext context, Object object, OutputStream output, NamespaceContext nsContext) throws JAXBException {
            // TODO: handle nsContext
            ((MarshallerBridgeContext)context).m.marshal(object,output);
        }

        public void marshal(BridgeContext context, Object object, Node output) throws JAXBException {
            ((MarshallerBridgeContext)context).m.marshal(object,output);
        }

        public void marshal(BridgeContext context, Object object, ContentHandler contentHandler) throws JAXBException {
            ((MarshallerBridgeContext)context).m.marshal(object,contentHandler);
        }

        public void marshal(BridgeContext context, Object object, Result result) throws JAXBException {
            ((MarshallerBridgeContext)context).m.marshal(object,result);
        }

        public Object unmarshal(BridgeContext context, XMLStreamReader in) {
            throw new UnsupportedOperationException();
        }

        public Object unmarshal(BridgeContext context, Source in) {
            throw new UnsupportedOperationException();
        }

        public Object unmarshal(BridgeContext context, InputStream in) {
            throw new UnsupportedOperationException();
        }

        public TypeReference getTypeReference() {
            throw new UnsupportedOperationException();
        }
    };
}
