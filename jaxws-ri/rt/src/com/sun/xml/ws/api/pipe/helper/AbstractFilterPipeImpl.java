package com.sun.xml.ws.api.pipe.helper;

import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.message.Packet;

/**
 * Default implementation of {@link Pipe} that is used as a filter.
 *
 * <p>
 * A filter pipe works on a {@link Packet}, then pass it onto the next pipe.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractFilterPipeImpl extends AbstractPipeImpl {
    /**
     * Next pipe to call.
     */
    protected final Pipe next;

    protected AbstractFilterPipeImpl(Pipe next) {
        this.next = next;
        assert next!=null;
    }

    protected AbstractFilterPipeImpl(AbstractFilterPipeImpl that, PipeCloner cloner) {
        super(that, cloner);
        this.next = cloner.copy(that.next);
        assert next!=null;
    }

    public Packet process(Packet packet) {
        return next.process(packet);
    }

    @Override
    public void preDestroy() {
        next.preDestroy();
    }
}
