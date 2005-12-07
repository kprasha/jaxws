package com.sun.xml.ws.sandbox.pipe;

import com.sun.xml.ws.sandbox.message.Message;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;

/**
 * Abstraction of the intermediate layers in the processing chain
 * and transport.
 *
 * <h2>What is a {@link Pipe}?</h2>
 * <p>
 * Transport is a kind of pipe. It sends the {@link Message}
 * through, say, HTTP connection, and receives the data back into another {@link Message}.
 *
 * <p>
 * More often, a pipe is a filter. It acts on a message,
 * and then it passes the message into another pipe. It can
 * do the same on the way back.
 *
 * <p>
 * For example, XWSS will be a {@link Pipe}
 * that delegates to another {@link Pipe}, and it can wrap a {@link Message} into
 * another {@link Message} to encrypt the body and add a header, for example.
 *
 * <p>
 * Yet another kind of filter pipe is those that wraps {@link LogicalHandler}
 * and {@link SOAPHandler}. These pipes are heavy-weight; they often consume
 * a message and create a new one, and then pass it to the next pipe.
 * For performance reason it probably makes sense to have one {@link Pipe}
 * instance that invokes a series of {@link LogicalHandler}s, another one
 * for {@link SOAPHandler}.
 *
 * <p>
 * There would be a {@link Pipe} implementation that invokes {@link Provider}.
 * There would be a {@link Pipe} implementation that invokes a service method
 * on the user's code.
 * There would be a {@link Dispatch} implementation that invokes a {@link Pipe}.
 *
 * <p>
 * WS-MEX can be implemented as a {@link Pipe} that looks for
 * {@link Message#getPayloadNamespaceURI()} and serves the request.
 *
 *
 * <h2>Pipe Lifecycle</h2>
 * <p>
 * {@link Pipe} list is expensive to set up, so once it's created it will be reused.
 * A {@link Pipe} list is not reentrant; one pipe is used to process one request/response
 * at at time. This allows a {@link Pipe} implementation to cache thread-specific resource
 * (such as a buffer, temporary array, or JAXB Unmarshaller.)
 *
 * 
 *
 * <h2>Pipes and Handlers</h2>
 * <p>
 * JAX-WS has a notion of {@link LogicalHandler} and {@link SOAPHandler}, and
 * we intend to have one {@link Pipe} implementation that invokes all the
 * {@link LogicalHandler}s and another {@link Pipe} implementation that invokes
 * all the {@link SOAPHandler}s. Those implementations need to convert a {@link Message}
 * into an appropriate format, but grouping all the handlers together eliminates
 * the intermediate {@link Message} instanciation between such handlers.
 * <p>
 * This grouping also allows such implementations to follow the event notifications
 * to handlers (i.e. {@link Handler#close(MessageContext)} method.
 *
 *
 * TODO: needs more thinking about how channel pipe is created.
 * it's different between the client and the server.
 *
 * TODO: in one-way we want to send reply before the processing starts.
 *
 * TODO: do we need to distinguish between inbound and outbound?
 *       this relates to the point of should we have different representations
 *       for a message that is created and a message that is processed?
 *
 * TODO: Possible types of channel:
 *      creator: create message from wire
 *          to SAAJ SOAP message
 *          to cached representation
 *          directly to JAXB beans
 *      transformer: transform message from one representation to another
 *          JAXB beans to encoded SOAP message
 *          StAX writing + JAXB bean to encoded SOAP message
 *      modifier: modify message
 *          add SOAP header blocks
 *          security processing
 *      header block processor:
 *          process certain SOAP header blocks
 *      outbound initiator: input from the client
 *          Manage input e.g. JAXB beans and associated with parts of the SOAP message
 *      inbound invoker: invoke the service
 *         Inkoke SEI, e.g. EJB or SEI in servlet.
 */
public interface Pipe {
    /**
     * Invoked after the pipe chain is set up to give {@link Pipe}s a chance
     * to initialize themselves.
     *
     * This can be used to invoke {@link PostConstruct} lifecycle methods
     * on user handler.
     *
     * TODO: most likely we want this to take some parameter so that
     * channel can find out the environment it lives in.
     */
    void postConstruct();

    /**
     * Sends a {@link Message} and returns a response {@link Message} to it.
     *
     * @throws WebServiceException
     *      Inside the server, this signals an error condition where
     *      a fault reply is in order (or the exception gets eaten by
     *      the top-most transport {@link Pipe} if it's one-way.)
     *      This frees each {@link Pipe} from try/catching a
     *      {@link WebServiceException} in every layer.
     *
     *      Note that this method is also allowed to return a {@link Message}
     *      that has a fault as the payload.
     *
     * @throws RuntimeException
     *      Other runtime exception thrown by this method must
     *      be treated as a bug in the channel implementation,
     *      and therefore should not be converted into a fault.
     *      Otherwise it becomes very difficult to debug implementation
     *      problems.
     *
     *      The consequence of this is that if a channel calls
     *      into an user application (such as {@link SOAPHandler}
     *      or {@link LogicalHandler}), where a {@link RuntimeException}
     *      is *not* a bug in the JAX-WS implementation, it must be catched
     *      and wrapped into a {@link WebServiceException}.
     *
     * TODO: we talke about designating a special exception class,
     * like <tt>AbortError</tt> to indicate a fatal situation where
     * we just have to bail out. The idea is to prohibit any {@link Pipe}
     * from eating it.
     *
     * @param msg
     *      always a non-null valid unconsumed {@link Message}.
     *      The callee may consume a {@link Message} (and in fact
     *      most of the time it will), and therefore once a {@link Message}
     *      is given to a {@link Pipe}, the caller may not access
     *      its payload.
     *
     * @return
     *      always a non-null valid unconsumed {@link Message}.
     *      Even when we are processing one way, we'll still return
     *      an empty {@link Message} (not null), so that the channel
     *      implementations don't have to check for null.
     */
    Message process( Message msg );

    /**
     * Invokes before the pipe chain is about to be discarded,
     * to give {@link Pipe}s a chance to clean up any resources.
     *
     * This can be used to invoke {@link PreDestroy} lifecycle methods
     * on user handler. The invocation of it is optional on the client side.
     */
    void preDestroy();
}
