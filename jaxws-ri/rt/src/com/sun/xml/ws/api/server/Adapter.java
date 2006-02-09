package com.sun.xml.ws.api.server;

import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.WSEndpoint;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSEndpoint.PipeHead;
import com.sun.xml.ws.util.Pool;

/**
 * Receives incoming messages from a transport (such as HTTP, JMS, etc)
 * in a transport specific way, and delivers it to {@link WSEndpoint.PipeHead#process}.
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
public abstract class Adapter<TK extends Adapter.Toolkit> {
    protected final WSEndpoint<?> endpoint;

    public class Toolkit {
        public final Encoder encoder;
        public final Decoder decoder;
        public final PipeHead head;

        public Toolkit() {
            WSBinding binding = endpoint.getBinding();
            this.encoder = binding.createEncoder();
            this.decoder = binding.createDecoder();
            this.head = endpoint.createPipeHead();
        }
    }

    /**
     * Pool of {@link Toolkit}s.
     */
    protected final Pool<TK> pool = new Pool<TK>() {
        protected TK create() {
            return createToolkit();
        }
    };

    protected Adapter(WSEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Gets the endpoint that this {@link Adapter} is serving.
     */
    public WSEndpoint<?> getEndpoint() {
        return endpoint;
    }

    /**
     * Creates a {@link Toolkit} instance.
     *
     * <p>
     * If the derived class doesn't have to add any per-thread state
     * to {@link Toolkit}, simply implement this as {@code new Toolkit()}.
     */
    protected abstract TK createToolkit();
}
