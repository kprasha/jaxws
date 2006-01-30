package com.sun.xml.ws.client.port;

import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.MessageProperties;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.client.RequestContext;
import com.sun.xml.ws.encoding.soap.DeserializationException;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.model.ParameterImpl;
import com.sun.xml.ws.model.WrapperParameter;
import com.sun.xml.ws.sandbox.message.impl.jaxb.JAXBMessage;
import com.sun.xml.ws.util.Pool;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;
import java.io.StringWriter;
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

    private final Boolean isOneWay;

    private final SEIModel seiModel;

    /**
     * Used to get a value from method invocation parameter.
     *
     * valueGetters[i] is for methodArgs[i], and so on.
     */
    /*package*/ final ValueGetter[] valueGetters;

    private final ResponseBuilder responseBuilder;

    public SyncMethodHandler(PortInterfaceStub owner, JavaMethodImpl method) {
        super(owner);

        this.soapAction = '"'+method.getBinding().getSOAPAction()+'"';
        this.seiModel = owner.seiModel;

        {// prepare objects for creating messages
            List<ParameterImpl> rp = method.getRequestParameters();

            BodyBuilder bodyBuilder = null;
            List<MessageFiller> fillers = new ArrayList<MessageFiller>();
            valueGetters = new ValueGetter[rp.size()];

            for (ParameterImpl param : rp) {
                ValueGetter getter = ValueGetter.get(param);

                switch(param.getInBinding().kind) {
                case BODY:
                    if(param.isWrapperStyle()) {
                        if(param.getParent().getBinding().isRpcLit())
                            bodyBuilder = new BodyBuilder.RpcLit((WrapperParameter)param,owner);
                        else
                            bodyBuilder = new BodyBuilder.DocLit((WrapperParameter)param,owner);
                    } else {
                        bodyBuilder = new BodyBuilder.Bare(param,owner);
                    }
                    break;
                case HEADER:
                    fillers.add(new MessageFiller.Header(
                        owner.seiModel,
                        param.getIndex(),
                        owner.soapVersion,
                        param.getBridge(),
                        getter ));
                    break;
                case ATTACHMENT:
                    // TODO: implement this later
                    throw new UnsupportedOperationException();
                case UNBOUND:
                    break;
                default:
                    throw new AssertionError(); // impossible
                }
            }

            if(bodyBuilder==null) {
                // no parameter binds to body. we create an empty message
                switch(owner.soapVersion) {
                case SOAP_11:
                    bodyBuilder = BodyBuilder.EMPTY_SOAP11;
                    break;
                case SOAP_12:
                    bodyBuilder = BodyBuilder.EMPTY_SOAP12;
                    break;
                default:
                    throw new AssertionError();
                }
            }

            this.bodyBuilder = bodyBuilder;
            this.inFillers = fillers.toArray(new MessageFiller[fillers.size()]);
        }

        {// prepare objects for processing response
            List<ParameterImpl> rp = method.getResponseParameters();
            List<ResponseBuilder> builders = new ArrayList<ResponseBuilder>();

            for( ParameterImpl param : rp ) {
                ValueSetter setter = ValueSetter.get(param);
                switch(param.getOutBinding().kind) {
                case BODY:
                    if(param.isWrapperStyle()) {
                        if(param.getParent().getBinding().isRpcLit())
                            builders.add(new ResponseBuilder.RpcLit((WrapperParameter)param));
                        else
                            builders.add(new ResponseBuilder.DocLit((WrapperParameter)param));
                    } else {
                        builders.add(new ResponseBuilder.Body(param.getBridge(),setter));
                    }
                    break;
                case HEADER:
                    builders.add(new ResponseBuilder.Header(param, setter));
                    break;
                case ATTACHMENT:
                    // TODO: implement this later
                    throw new UnsupportedOperationException();
                case UNBOUND:
                    builders.add(new ResponseBuilder.NullSetter(setter,
                        ResponseBuilder.getVMUninitializedValue(param.getTypeReference().type)));
                    break;
                default:
                    throw new AssertionError();
                }
            }

            switch(builders.size()) {
            case 0:
                responseBuilder = ResponseBuilder.NONE;
                break;
            case 1:
                responseBuilder = builders.get(0);
                break;
            default:
                responseBuilder = new ResponseBuilder.Composite(builders);
            }
        }

        this.isOneWay = method.getMEP().isOneWay();
    }

    public Object invoke(Object proxy, Object[] args, RequestContext rc) throws WebServiceException {

        Pool.Marshaller pool = seiModel.getMarshallerPool();

        Marshaller m = pool.take();

        try {
            Message msg = createRequestMessage(args);

            MessageProperties props = msg.getProperties();
            props.proxy = owner;
            props.soapAction = soapAction;
            props.isOneWay = isOneWay;
            props.endpointAddress = owner.endpointAddress;

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

            if(reply==null)
                // no reply. must have been one-way
                return null;

            if(reply.isFault()) {
                // TODO: data-bind fault into exception
                try {
                    StringWriter w = new StringWriter();
                    XMLStreamWriter sw = XMLOutputFactory.newInstance().createXMLStreamWriter(w);
                    reply.writeTo(sw);
                    sw.close();
                    throw new UnsupportedOperationException("Fault not implemented yet\n"+w.toString());
                } catch (XMLStreamException e) {
                    throw new Error(e);
                }
                //try {
                //    throw new SOAPFaultException(reply.readAsSOAPMessage().getSOAPBody().getFault());
                //} catch (SOAPException e) {
                //    // TODO: i18n
                //    throw new WebServiceException("Unable to read in a SOAP fault message",e);
                //}
            } else {
                BridgeContext context = seiModel.getBridgeContext();
                try {
                    return responseBuilder.readResponse(reply,args,context);
                } catch (JAXBException e) {
                    throw new DeserializationException("failed.to.read.response",e);
                } catch (XMLStreamException e) {
                    throw new DeserializationException("failed.to.read.response",e);
                }
            }
        } finally {
            pool.recycle(m);
        }
    }

    /**
     * Creates a request {@link JAXBMessage} from method arguments.
     *
     */
    private Message createRequestMessage(Object[] args) {
        Message msg = bodyBuilder.createMessage(args);

        for (MessageFiller filler : inFillers)
            filler.fillIn(args,msg);

        return msg;
    }
}
