package com.sun.xml.ws.api.pipe.helper;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.TubeCloner;

/**
 * {@link Tube} that invokes {@link Pipe}.
 *
 * <p>
 * This can be used to make a {@link Pipe} look like a {@link Tube}.
 *
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
public class PipeAdapter extends AbstractTubeImpl {
    private final Pipe next;

    public static Tube adapt(Pipe p) {
        if (p instanceof Tube) {
            return (Tube) p;
        } else {
            return new PipeAdapter(p);
        }
    }


    private PipeAdapter(Pipe next) {
        this.next = next;
    }

    /**
     * Copy constructor
     */
    private PipeAdapter(PipeAdapter that, TubeCloner cloner) {
        super(that,cloner);
        this.next = ((PipeCloner)cloner).copy(that.next);
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

    @NotNull
    public NextAction processException(@NotNull Throwable t) {
        throw new IllegalStateException();
    }

    public void preDestroy() {
        next.preDestroy();
    }

    public PipeAdapter copy(TubeCloner cloner) {
        return new PipeAdapter(this,cloner);
    }
}
