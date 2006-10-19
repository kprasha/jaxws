package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.api.message.Message;

import javax.xml.stream.XMLStreamReader;

/**
 * TODO javadoc
 * 
 * @author Jitendra Kotamraju
 *
 */
public interface StreamSOAPCodec extends Codec {
    public Message decode(XMLStreamReader reader);
}
