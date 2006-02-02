package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.stream.PropertyManager;
import com.sun.xml.stream.writers.XMLStreamWriterImpl;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Encoder;

import javax.activation.DataHandler;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.channels.WritableByteChannel;
import java.util.UUID;

/**
 * Partial Implmentation of MTOM/XOP {@link Encoder}.
 * <p/>
 * This will typically be created by the
 * {@link com.sun.xml.ws.transport.http.client.HttpTransportPipe} or
 * {@link com.sun.xml.ws.transport.local.client.LocalTransportPipe} on the client side and
 * by {@link com.sun.xml.ws.api.pipe.Acceptor} on the server side.
 *
 * @author Vivek Pandey
 */
public class XOPEncoder extends XMLStreamWriterImpl implements Encoder {
    private final SOAPVersion version;
    private final String boundary = "boundary=\"uuid:" + UUID.randomUUID().toString() + "\";";
    private final String soap11ContentType = "\"text/xml\"";
    private final String soap12ContentType = "\"application/soap+xml\"";

    public XOPEncoder(SOAPVersion version, OutputStream outputStream, PropertyManager propertyManager) throws IOException {
        super(outputStream, propertyManager);
        this.version = version;
    }

    public XOPEncoder(SOAPVersion version, OutputStream outputStream, String s, PropertyManager propertyManager) throws IOException {
        super(outputStream, s, propertyManager);
        this.version = version;
    }

    public XOPEncoder(SOAPVersion version, Writer writer, PropertyManager propertyManager) throws IOException {
        super(writer, propertyManager);
        this.version = version;
    }

    /**
     * Return the soap 1.1 and soap 1.2 specific XOP packaged ContentType
     *
     * @return A non-null content type for soap11 or soap 1.2 content type
     */
    public String getStaticContentType() {
        if (version.compareTo(SOAPVersion.SOAP_11) == 0) {
            return "Multipart/Related;type=\"application/xop+xml\";type" +
                    soap11ContentType + ";" + boundary + "start-info=" + soap11ContentType;
        } else {
            return "Multipart/Related;type=\"application/xop+xml\";type" +
                    soap12ContentType + ";" + boundary + "start-info=" + soap12ContentType;
        }
    }

    public String encode(Packet packet, OutputStream out) throws IOException {
        throw new UnsupportedOperationException();
    }

    public String encode(Packet packet, WritableByteChannel buffer) {
        throw new UnsupportedOperationException();
    }

    //TODO
    public Encoder copy() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //TODO
    public void writeBinary(byte[] data, int start, int len, String contentType) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    //TODO
    public void writeBinary(DataHandler data) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    //TODO
    public OutputStream writeBinary(String contentType) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }
}
