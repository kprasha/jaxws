package com.sun.xml.ws.sandbox.notes;

import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractPipeImpl;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterPipeImpl;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.Packet;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.WebServiceException;
import java.util.Map;
import java.util.HashMap;

/**
 * {@link Pipe} that looks at &lt;wsa:Action> header
 * and dispatches {@link Message}s to the right component.
 *
 * <p>
 * Many web service standards define a "protocol message",
 * which is delivered to the same application endpoint but
 * is expected to be processed by the messaging infrastructure.
 *
 * <p>
 * {@link WsaActionDispatchPipe} allows such technologies to register
 * their own {@link Pipe}s against a particular action URI.
 * When {@link WsaActionDispatchPipe} receives a {@link Message}
 * with &lt;wsa:Action> header, it checks for this header and
 * invokes the {@link Pipe} registered.
 *
 * <p>
 * If a {@link Message} has no action header or no {@link Pipe} is
 * registered against it, then it's simply passed to the "next"
 * {@link Pipe}.
 *
 * <p>
 * Registration is done in terms of adding {@link Pipe}s to
 * a {@link Map}. It is up to a pipe assembler to provide
 * other {@link Pipe}s an opportunity to register themselves.
 *
 * @author Kohsuke Kawaguchi
 */
public final class WsaActionDispatchPipe extends AbstractFilterPipeImpl {
    /**
     * wsa:Action URIs to {@link Pipe}s that process such messages.
     */
    private final Map<String,Pipe> receivers;

    /**
     * Creates a fresh {@link WsaActionDispatchPipe}.
     *
     * @param next
     *      This pipe receives messages when no interesting
     *      wsa:Action value is found. Must not be null.
     * @param actionReceivers
     *      A map that has a list of {@link Pipe}s that are
     *      interested in processing messages with particular
     *      <tt>wsa:Action</tt>s.
     *
     *      <p>
     *      This map doesn't have to be completely filled
     *      by the time this constructor is invoked, but it
     *      must be filled before the first message is processed.
     *      (This allows pipes created after this pipe to be
     *      registered.)
     */
    // TODO: if this moves to Tango, take PipeConfiguration instead.
    public WsaActionDispatchPipe(Pipe next, Map<String,Pipe> actionReceivers) {
        super(next);
        assert actionReceivers!=null;
        this.receivers = actionReceivers;
    }

    /**
     * Copy-constructor.
     */
    private WsaActionDispatchPipe(WsaActionDispatchPipe that,PipeCloner cloner) {
        super(that,cloner);
        this.receivers = new HashMap<String, Pipe>(that.receivers.size());
        for (Map.Entry<String, Pipe> e : that.receivers.entrySet())
            receivers.put(e.getKey(), cloner.copy(e.getValue()));
    }

    public Packet process(Packet packet) {
        Header actionHeader = packet.getMessage().getHeaders().get(
            "http://schemas.xmlsoap.org/ws/2004/08/addressing/", "Action");

        if(actionHeader==null)
            // no action header. pass through to the next pipe
            return next.process(packet);

        String action = parseAction(actionHeader).trim();

        // pick the pipe to invoke
        Pipe pipe = receivers.get(action);
        if(pipe==null)
            // nobody showed interest. pass through.
            return next.process(packet);

        // terminate the normal message processing
        // and give it to the pipe that volunteered.
        return pipe.process(packet);
    }

    /**
     * Parses wsa:Action header.
     *
     * @return
     *      the action URI.
     */
    private String parseAction(Header actionHeader) {
        String action;
        try {
            XMLStreamReader r = actionHeader.readHeader();
            action = r.getElementText();
            r.close();
        } catch (XMLStreamException e) {
            throw new WebServiceException(
                "Failed to parse wsa:Action header",
                e);
        }
        return action;
    }

    public Pipe copy(PipeCloner cloner) {
        return new WsaActionDispatchPipe(this,cloner);
    }
}
