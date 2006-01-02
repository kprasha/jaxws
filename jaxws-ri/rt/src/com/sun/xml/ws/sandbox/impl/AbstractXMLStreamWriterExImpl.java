package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.util.ByteArrayBuffer;

import javax.xml.stream.XMLStreamException;
import javax.activation.DataHandler;
import java.io.OutputStream;
import java.io.IOException;

import org.jvnet.staxex.XMLStreamWriterEx;

/**
 * Partial default implementation of {@link XMLStreamWriterEx}.
 *
 * TODO: find a good home for this class.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractXMLStreamWriterExImpl implements XMLStreamWriterEx {

    private StreamImpl stream;

    public void writeBinary(DataHandler data) throws XMLStreamException {
        try {
            StreamImpl stream = _writeBinary(data.getContentType());
            stream.write(data.getInputStream());
            stream.close();
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
    public OutputStream writeBinary(String contentType) throws XMLStreamException {
        return _writeBinary(contentType);
    }

    private StreamImpl _writeBinary(String contentType) {
        if(stream==null)
            stream = new StreamImpl();
        else
            stream.reset();
        stream.contentType = contentType;
        return stream;
    }

    private final class StreamImpl extends ByteArrayBuffer {
        private String contentType;
        public void close() throws IOException {
            super.close();
            try {
                writeBinary(buf,0,size(),contentType);
            } catch (XMLStreamException e) {
                IOException x = new IOException();
                x.initCause(e);
                throw x;
            }
        }
    }
}
