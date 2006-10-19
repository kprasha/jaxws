package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.api.message.Message;
import com.sun.istack.NotNull;

import javax.xml.stream.XMLStreamReader;

/**
 * Reads events from {@link XMLStreamReader} and constructs a
 * {@link Message} for SOAP envelope. {@link Codecs} allows a
 * way to construct a whole codec that can handle MTOM, MIME
 * encoded packages using this codec.
 *
 * @see {@link Codecs}
 * 
 * @author Jitendra Kotamraju
 */
public interface StreamSOAPCodec extends Codec {
    /**
     * Reads events from {@link XMLStreamReader} and constructs a
     * {@link Message} for SOAP envelope.
     *
     * @param reader that represents SOAP envelope infoset
     * @return a {@link Message} for SOAP envelope
     */
    public @NotNull Message decode(@NotNull XMLStreamReader reader);
}
