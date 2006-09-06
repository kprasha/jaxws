package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.api.message.Packet;

/**
 * Convenient default implementation for filtering {@link Tube}.
 *
 * <p>
 * In this prototype, this is not that convenient, but in the real production
 * code where we have {@code preDestroy()} and {@code clone()}, this
 * is fairly handy.
 * 
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractFilterTube extends AbstractTube {
    protected final Tube next;

    public AbstractFilterTube(Tube next) {
        this.next = next;
    }

    /**
     * Default no-op implementation.
     */
    public NextAction processRequest(Packet p) {
        return doInvoke(next,p);
    }

    /**
     * Default no-op implementation.
     */
    public NextAction processResponse(Packet p) {
        return doReturnWith(p);
    }
}
