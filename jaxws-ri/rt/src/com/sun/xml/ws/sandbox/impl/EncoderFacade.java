package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.api.pipe.Encoder;
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
    private final Encoder encoder;

    public EncoderFacade(SOAPVersion version, SOAPBinding binding) {
        if(binding.isMTOMEnabled()){
            encoder = MtomEncoder.get(version);
        }else{
            encoder = TestEncoderImpl.get(version);
        }
    }

    public String getStaticContentType() {
        return encoder.getStaticContentType();
    }

    public String encode(Packet packet, OutputStream out) throws IOException {
        return encoder.encode(packet, out);
    }

    public String encode(Packet packet, WritableByteChannel buffer) {
        return encoder.encode(packet, buffer);
    }

    public Encoder copy() {
        return encoder.copy();
    }
}
