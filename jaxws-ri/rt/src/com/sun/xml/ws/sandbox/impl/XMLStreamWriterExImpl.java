package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.sandbox.XMLStreamWriterEx;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

/**
 * {@link XMLStreamWriterEx} implementation that just delegates to {@link XMLStreamWriter},
 * where binary data is simply written as base64-encoded text.
 *
 * TODO: find a good home for this class.
 *
 * @author Kohsuke Kawaguchi
 */
public final class XMLStreamWriterExImpl extends AbstractXMLStreamWriterExImpl {
    private final XMLStreamWriter base;

    public XMLStreamWriterExImpl(XMLStreamWriter base) {
        this.base = base;
    }

    public XMLStreamWriter getBase() {
        return base;
    }

    public void writeBinary(byte[] data, int start, int len, String mimeType) throws XMLStreamException {
        // TODO : convert to base64
        getBase().writeCharacters("TODO:some-base64-encoded-string");
    }
}
