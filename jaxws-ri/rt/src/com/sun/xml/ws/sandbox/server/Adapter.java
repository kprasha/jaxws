package com.sun.xml.ws.sandbox.server;

import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.message.Packet;

/**
 * Receives incoming messages from a transport (such as HTTP, JMS, etc)
 * in a transport specific way, and delivers it to {@link WSEndpoint#process(Packet)}.
 *
 * <p>
 * Since this class mostly concerns itself with converting a
 * transport-specific message representation to a {@link Packet},
 * the name is the "adapter".
 *
 * <p>
 * This class contains a bunch of convenience methods and refererences
 * to other components that aid the adapting process, such as ...
 *
 * <ol>
 * <li>
 * {@link Encoder} and {@link Decoder}, which achieves
 * the de-coupling of transport and message encoding.
 * </ol>
 *
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Adapter {
    protected final WSEndpoint head;

    protected Adapter(WSEndpoint head) {
        this.head = head;
    }
}
