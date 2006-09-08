package com.sun.xml.ws.api.pipe.helper;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.Tube;

/**
 * Base class for {@link Tube} implementation.
 *
 * <p>
 * This can be also used as a {@link Pipe}, and thus effectively
 * making every {@link Tube} usable as a {@link Pipe}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractTubeImpl implements Tube, Pipe {
    /**
     * Instance resued for better performance.
     */
    private final NextAction na = new NextAction();

    /**
     * Default constructor.
     */
    protected AbstractTubeImpl() {
    }

    /**
     * Copy constructor.
     */
    protected AbstractTubeImpl(AbstractTubeImpl that, PipeCloner cloner) {
        cloner.add(that,this);
    }

    protected final NextAction doInvoke(Tube next, Packet packet) {
        na.invoke(next,packet);
        return na;
    }

    protected final NextAction doInvokeAndForget(Tube next, Packet packet) {
        na.invokeAndForget(next,packet);
        return na;
    }

    protected final NextAction doReturnWith(Packet packet) {
        na.returnWith(packet);
        return na;
    }

    protected final NextAction doSuspend() {
        na.suspend();
        return na;
    }

    /**
     * "Dual stack" compatibility mechanism.
     * Allows {@link Tube} to be invoked from a {@link Pipe}.
     */
    public Packet process(Packet p) {
        return Fiber.current().runSync(this,p);
    }

    /**
     * Needs to be implemented by the derived class, but we can't make it abstract
     * without upsetting javac.
     */
    public AbstractTubeImpl copy(PipeCloner cloner) {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }
}
