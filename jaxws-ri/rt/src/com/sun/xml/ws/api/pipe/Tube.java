package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.api.message.Packet;

/**
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
public interface Tube {
    NextAction processRequest(Packet p);
    NextAction processResponse(Packet p);
}
