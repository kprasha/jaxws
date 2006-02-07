package com.sun.xml.ws.sandbox.server;

import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.spi.runtime.WSConnection;

/**
 * {@link Adapter} that receives messages in HTTP.
 *
 * @author Kohsuke Kawaguchi
 */
public class HttpAdapter extends Adapter {
    public HttpAdapter(WSEndpoint head) {
        super(head);
    }

    /**
     * Receives the incoming HTTP connection and dispatches
     * it to JAX-WS.
     *
     * <p>
     * This method is invoked by the lower-level HTTP stack,
     * and "connection" here is an HTTP connection.
     */
    public void handle(WSConnection connection) {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }
}
