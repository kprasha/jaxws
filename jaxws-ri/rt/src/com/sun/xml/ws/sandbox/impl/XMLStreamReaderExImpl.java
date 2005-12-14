package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.sandbox.XMLStreamReaderEx;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

/**
 * Default implementation of {@link XMLStreamReaderEx}
 * that simply wraps {@link XMLStreamReader}.
 *
 * @author Kohsuke Kawaguchi
 */
public class XMLStreamReaderExImpl implements XMLStreamReaderEx {
    private final XMLStreamReader core;

    public XMLStreamReaderExImpl(XMLStreamReader core) {
        this.core = core;
    }

    public XMLStreamReader getBase() {
        return core;
    }

    public CharSequence getText() throws XMLStreamException {
        return core.getText();
    }
}
