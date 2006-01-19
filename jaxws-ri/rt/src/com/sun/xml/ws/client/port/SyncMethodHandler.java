package com.sun.xml.ws.client.port;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.MessageProperties;
import com.sun.xml.ws.api.model.JavaMethod;
import com.sun.xml.ws.api.model.Parameter;
import com.sun.xml.ws.client.RequestContext;
import com.sun.xml.ws.model.WrapperParameter;
import com.sun.xml.ws.api.model.soap.SOAPBinding;
import com.sun.xml.ws.sandbox.message.impl.jaxb.JAXBMessage;

import javax.xml.bind.Marshaller;
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
 * At the construction time, we prepare {@link BodyBuilder} and {@link MessageFiller}s
 * that know how to move arguments into a {@link Message}.
 * Some arguments go to the payload, some go to headers, still others go to attachments.
 *
 * @author Kohsuke Kawaguchi
 */
final class SyncMethodHandler extends MethodHandler {

    // these objects together create a message from method parameters
    private final BodyBuilder bodyBuilder;
    private final MessageFiller[] inFillers;

    private final String soapAction;

    /**
     * Used to get a value from method invocation parameter.
     *
     * valueGetters[i] is for methodArgs[i], and so on.
     */
    /*package*/ final ValueGetter[] valueGetters;

    public SyncMethodHandler(PortInterfaceStub owner, JavaMethod method) {
        super(owner);

        this.soapAction = '"'+((SOAPBinding)method.getBinding()).getSOAPAction()+'"';

        {// prepare objects for creating messages
            List<Parameter> rp = method.getRequestParameters();

            BodyBuilder bodyBuilder = null;
            List<MessageFiller> fillers = new ArrayList<MessageFiller>();
            valueGetters = new ValueGetter[rp.size()];

            for (Parameter param : rp) {
                ValueGetter getter = ValueGetter.get(param);

                switch(param.getInBinding().kind) {
                case BODY:
                    if(param.isWrapperStyle()) {
                        bodyBuilder = new BodyBuilder.Wrapped((WrapperParameter)param,owner.model);
                    } else {
                        bodyBuilder = new BodyBuilder.Bare(param,owner.model);
                    }
                    break;
                case HEADER:
                    fillers.add(new MessageFiller.Header(
                        this,
                        param.getIndex(),
                        owner.soapVersion,
                        param.getBridge(),
                        getter ));
                case ATTACHMENT:
                    // TODO: implement this later
                    throw new UnsupportedOperationException();
                default:
                    throw new AssertionError(); // impossible
                }
            }

            assert bodyBuilder!=null;
            this.bodyBuilder = bodyBuilder;
            this.inFillers = fillers.toArray(new MessageFiller[fillers.size()]);
        }

        {// prepare objects for processing response

        }
    }

    public Object invoke(Object proxy, Object[] args, RequestContext rc) throws WebServiceException {

        Marshaller m = owner.marshallers.take();

        try {
            Message msg = createRequestMessage(args);

            MessageProperties props = msg.getProperties();
            props.proxy = proxy;
            props.requestContext = rc;
            props.soapAction = soapAction;

            // TODO: fill in MessageProperties
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
     */
    private Message createRequestMessage(Object[] args) {
        Message msg = new JAXBMessage(
            bodyBuilder.bridge, bodyBuilder.build(args),
            owner.model.getBridgeContext(), owner.soapVersion );

        for (MessageFiller filler : inFillers)
            filler.fillIn(args,msg);

        return msg;
    }
}
