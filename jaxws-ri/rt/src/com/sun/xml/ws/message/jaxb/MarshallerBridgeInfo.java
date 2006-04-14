/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.message.jaxb;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.bind.api.TypeReference;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.io.InputStream;
import java.io.OutputStream;

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
            Marshaller m = ((MarshallerBridgeContext) context).m;
            m.setProperty(Marshaller.JAXB_FRAGMENT,true);
            try {
                m.marshal(object,output);
            } finally {
                m.setProperty(Marshaller.JAXB_FRAGMENT,false);
            }
        }

        public void marshal(BridgeContext context, Object object, OutputStream output, NamespaceContext nsContext) throws JAXBException {
            // TODO: handle nsContext
            Marshaller m = ((MarshallerBridgeContext) context).m;
            m.setProperty(Marshaller.JAXB_FRAGMENT,true);
            try {
                m.marshal(object,output);
            } finally {
                m.setProperty(Marshaller.JAXB_FRAGMENT,false);
            }
        }

        public void marshal(BridgeContext context, Object object, Node output) throws JAXBException {
            Marshaller m = ((MarshallerBridgeContext) context).m;
            m.setProperty(Marshaller.JAXB_FRAGMENT,true);
            try {
                m.marshal(object,output);
            } finally {
                m.setProperty(Marshaller.JAXB_FRAGMENT,false);
            }
        }

        public void marshal(BridgeContext context, Object object, ContentHandler contentHandler) throws JAXBException {
            Marshaller m = ((MarshallerBridgeContext) context).m;
            m.setProperty(Marshaller.JAXB_FRAGMENT,true);
            try {
                m.marshal(object,contentHandler);
            } finally {
                m.setProperty(Marshaller.JAXB_FRAGMENT,false);
            }
        }

        public void marshal(BridgeContext context, Object object, Result result) throws JAXBException {
            Marshaller m = ((MarshallerBridgeContext) context).m;
            m.setProperty(Marshaller.JAXB_FRAGMENT,true);
            try {
            } finally {
                m.setProperty(Marshaller.JAXB_FRAGMENT,false);
            }
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

        public Object unmarshal(BridgeContext context, Node n) {
            throw new UnsupportedOperationException();
        }

        public TypeReference getTypeReference() {
            throw new UnsupportedOperationException();
        }
    };
}
