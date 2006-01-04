package com.sun.xml.ws.client.port;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.CompositeStructure;
import com.sun.xml.ws.model.JavaMethod;
import com.sun.xml.ws.model.Parameter;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.message.MessageProperties;
import com.sun.xml.ws.sandbox.message.impl.jaxb.JAXBMessage;

import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link MethodHandler} that handles synchronous method invocations.
 *
 * <p>
 * This class mainly performs the following two tasks:
 * <ol>
 *  <li>Accepts Object[] that represents arguments for a Java method,
 *      and creates {@link JAXBMessage} that represents a request message.
 *  <li>Takes a {@link Message] that represents a response,
 *      and extracts the return value (and updates {@link Holder}s.)
 * </ol>
 *
 * <h2>Creating {@link JAXBMessage}</h2>
 * <p>
 * At the construction time, we prepare {@link BodySetter}s and {@link MessageFiller}s
 * that know how to move arguments into a {@link Message}.
 * Some arguments go to the payload, some go to headers, still others go to attachments.
 *
 * @author Kohsuke Kawaguchi
 */
final class SyncMethodHandler extends MethodHandler {

    private final QName operationName;

    /**
     * Specifies how to data-bind payload parameters.
     */
    private final Bridge[] parameterBridges;

    // these objects together create a message from method parameters
    private final BodySetter[] inSetters;
    private final MessageFiller[] inFillers;

    /**
     * Used to get a value from method invocation parameter.
     *
     * valueGetters[i] is for methodArgs[i], and so on.
     */
    /*package*/ final ValueGetter[] valueGetters;

    public SyncMethodHandler(PortInterfaceStub owner, JavaMethod method) {
        super(owner);

        this.operationName = new QName(owner.model.getTargetNamespace(), method.getOperationName() );

        {// prepare objects for creating messages
            List<Parameter> rp = method.getRequestParameters();

            List<Bridge> requestBodyBridges = new ArrayList<Bridge>();
            List<BodySetter> reqParamSetters = new ArrayList<BodySetter>();
            List<MessageFiller> fillers = new ArrayList<MessageFiller>();
            valueGetters = new ValueGetter[rp.size()];

            for (Parameter param : rp) {
                ValueGetter getter = ValueGetter.fromMode(param.getMode());

                switch(param.getInBinding().kind) {
                case BODY:
                    Bridge bridge = owner.model.getBridge(param.getTypeReference());
                    requestBodyBridges.add(bridge);

                    BodySetter setter = new BodySetter(
                        param.getIndex(), reqParamSetters.size(), getter);

                    reqParamSetters.add(setter);
                    break;
                case HEADER:
                    fillers.add(new MessageFiller.Header(
                        this,
                        param.getIndex(),
                        owner.soapVersion,
                        owner.model.getBridge(param.getTypeReference()),
                        getter ));
                case ATTACHMENT:
                    // TODO: implement this later
                    throw new UnsupportedOperationException();
                default:
                    throw new AssertionError(); // impossible
                }
            }

            this.inSetters = reqParamSetters.toArray(new BodySetter[reqParamSetters.size()]);
            this.inFillers = fillers.toArray(new MessageFiller[fillers.size()]);
            this.parameterBridges = requestBodyBridges.toArray(new Bridge[requestBodyBridges.size()]);
        }

        {// prepare objects for processing response

        }
    }

    public Object invoke(Object proxy, Object[] args) throws WebServiceException {

        Marshaller m = owner.marshallers.take();

        try {
            Message msg = createRequestMessage(args, m);

            MessageProperties props = msg.getProperties();
            props.proxy = proxy;
            // TODO: fill in MessageProperties
            //RequestContext requestContext = (RequestContext)(java.util.Map)((BindingProvider) _proxy).getRequestContext();
            //requestContext.put(JAXWS_CLIENT_HANDLE_PROPERTY, _proxy);
            //messageStruct.setMetaData(JAXWS_RUNTIME_CONTEXT, _rtcontext);
            //messageStruct.setMetaData(JAXWS_CONTEXT_PROPERTY, requestContext);
            //
            ////set mtom threshold value to
            //Object mtomThreshold = requestContext.get(MTOM_THRESHOLOD_VALUE);
            //messageStruct.setMetaData(MTOM_THRESHOLOD_VALUE, mtomThreshold);
            //// Initialize content negotiation property
            //ContentNegotiation.initialize(requestContext, messageStruct);

            //// Set MTOM processing for XML requests only
            //if (_rtcontext != null && _rtcontext.getModel() != null) {
            //    javax.xml.ws.soap.SOAPBinding sb = (binding instanceof javax.xml.ws.soap.SOAPBinding) ? (javax.xml.ws.soap.SOAPBinding) binding : null;
            //    if (sb != null) {
            //        _rtcontext.getModel().enableMtom(sb.isMTOMEnabled());
            //    }
            //}

            // process the message
            Message reply = owner.doProcess(msg);

            if(reply.isFault()) {
                // TODO: data-bind fault into exception
                throw new UnsupportedOperationException();
            } else {
                // TODO: handle normal response
                throw new UnsupportedOperationException();

                //BridgeContext bc = owner.bridgeContexts.take();
                //try {
                //    Object r = response.unmarshal(bc,reply.readPayload());
                //    owner.bridgeContexts.recycle(bc);
                //    return r;
                //} catch (JAXBException e) {
                //    // TODO: need to put a message saying "failed to unmarshal a response"
                //    throw new DeserializationException(e);
                //} catch (XMLStreamException e) {
                //    // TODO: need to put a message saying "failed to unmarshal a response"
                //    throw new XMLStreamReaderException(e);
                //}
            }
        } finally {
            owner.marshallers.recycle(m);
        }
    }

    /**
     * Creates a request {@link JAXBMessage} from method arguments.
     *
     * @param m
     *      The marshalled borrowed from a pool.
     */
    private Message createRequestMessage(Object[] args, Marshaller m) {
        CompositeStructure cs = new CompositeStructure();
        cs.bridges = parameterBridges;
        cs.values = new Object[parameterBridges.length];

        for (BodySetter bs : inSetters)
            bs.set(args,cs);
        Message msg = new JAXBMessage(m,operationName, CompositeStructure.class, cs, owner.soapVersion );

        for (MessageFiller filler : inFillers)
            filler.fillIn(args,msg);
        return msg;
    }
}
