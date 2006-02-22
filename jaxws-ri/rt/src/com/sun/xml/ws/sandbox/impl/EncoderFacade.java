package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.SOAPVersion;

import javax.xml.ws.soap.SOAPBinding;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * This is a facade to choose which {@link com.sun.xml.ws.api.pipe.Encoder} touse based on binding/properties. There
 * must be a better way to do this but for now lets keep it until we have better solition.
 *
 * @author Vivek Pandey
 */
public class EncoderFacade implements Encoder {
    private final Encoder mtomEncoder;
    private final Encoder soapEncoder;
    private final SOAPBinding binding;

    public EncoderFacade(SOAPVersion version, SOAPBinding binding) {
        mtomEncoder = MtomEncoder.get(version);
        soapEncoder = TestEncoderImpl.get(version);
        this.binding = binding;
    }

    public ContentType getStaticContentType(Packet packet) {
        return getEncoder().getStaticContentType(packet);
    }

    public ContentType encode(Packet packet, OutputStream out) throws IOException {
        return getEncoder().encode(packet, out);
    }

    public ContentType encode(Packet packet, WritableByteChannel buffer) {
        return getEncoder().encode(packet, buffer);
    }

    public Encoder copy() {
        return getEncoder().copy();
    }

    private Encoder getEncoder(){
        if(binding.isMTOMEnabled())
            return mtomEncoder;
        return soapEncoder;
    }
}
