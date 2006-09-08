package com.sun.xml.ws.api.pipe.helper;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.Tube;

/**
 * {@link Tube} that invokes {@link Pipe}.
 *
 * <p>
 * This can be used to make a {@link Pipe} look like a {@link Tube}.
 *
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
public class PipeAdapter extends AbstractTube {
    private final Pipe next;

    public PipeAdapter(Pipe next) {
        this.next = next;
    }

    /**
     * Uses the current fiber and runs the whole pipe to the completion
     * (meaning everything from now on will run synchronously.)
     */
    public NextAction processRequest(Packet p) {
        return doReturnWith(next.process(p));
    }

    public NextAction processResponse(Packet p) {
        throw new IllegalStateException();
    }

    public void preDestroy() {
        next.preDestroy();
    }

    public Pipe copy(PipeCloner cloner) {
        return next.copy(cloner);
    }
}
