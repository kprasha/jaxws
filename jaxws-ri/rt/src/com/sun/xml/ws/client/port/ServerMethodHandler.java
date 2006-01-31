package com.sun.xml.ws.client.port;

import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.MessageProperties;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.client.RequestContext;
import com.sun.xml.ws.encoding.soap.DeserializationException;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.model.ParameterImpl;
import com.sun.xml.ws.model.WrapperParameter;
import com.sun.xml.ws.sandbox.message.impl.jaxb.JAXBMessage;
import com.sun.xml.ws.server.RuntimeEndpointInfo;
import com.sun.xml.ws.util.Pool;
import java.lang.reflect.Method;

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
 *
 * <p>
 * This class mainly performs the following two tasks:
 * <ol>
 *  <li>Takes a {@link Message] that represents a request,
 *      and extracts the arguments (and updates {@link Holder}s.)
 *  <li>Accepts return value and {@link Holder} arguments for a Java method,
 *      and creates {@link JAXBMessage} that represents a response message.
 * </ol>
 *
 * <h2>Creating {@link JAXBMessage}</h2>
 * <p>
 * At the construction time, we prepare {@link BodyBuilder} and {@link MessageFiller}s
 * that know how to move arguments into a {@link Message}.
 * Some arguments go to the payload, some go to headers, still others go to attachments.
 *
 * @author Jitendra Kotamraju
 */
public final class ServerMethodHandler {

    // these objects together create a message from method parameters
    private final BodyBuilder bodyBuilder;
    private final MessageFiller[] inFillers;

    private final Boolean isOneWay;

    private final SEIModel seiModel;
    private final SOAPVersion soapVersion;
    private final Method method;

    /**
     * Used to get a value from method invocation parameter.
     *
     * valueGetters[i] is for methodArgs[i], and so on.
     */
    /*package*/ final ValueGetter[] valueGetters;

    private final ResponseBuilder responseBuilder;

    public ServerMethodHandler(RuntimeEndpointInfo endpointInfo, JavaMethodImpl method) {

        this.seiModel = endpointInfo.getRuntimeModel();
        soapVersion = endpointInfo.getBinding().getSOAPVersion();
        this.method = method.getMethod();

        {// prepare objects for creating messages
            List<ParameterImpl> rp = method.getResponseParameters();

            BodyBuilder bodyBuilder = null;
            List<MessageFiller> fillers = new ArrayList<MessageFiller>();
            valueGetters = new ValueGetter[rp.size()];

            for (ParameterImpl param : rp) {
                ValueGetter getter = ValueGetter.get(param);

                switch(param.getInBinding().kind) {
                case BODY:
                    if(param.isWrapperStyle()) {
                        if(param.getParent().getBinding().isRpcLit())
                            bodyBuilder = new BodyBuilder.RpcLit((WrapperParameter)param, seiModel, soapVersion);
                        else
                            bodyBuilder = new BodyBuilder.DocLit((WrapperParameter)param, seiModel, soapVersion);
                    } else {
                        bodyBuilder = new BodyBuilder.Bare(param, seiModel, soapVersion);
                    }
                    break;
                case HEADER:
                    fillers.add(new MessageFiller.Header(
                        seiModel,
                        param.getIndex(),
                        soapVersion,
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
                switch(soapVersion) {
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

        {// prepare objects for processing request
            List<ParameterImpl> rp = method.getRequestParameters();
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

    public Message invoke(Object proxy, Message req) {

        Pool.Marshaller pool = seiModel.getMarshallerPool();
        Marshaller m = pool.take();

        try {
            Object[] args = new Object[method.getParameterTypes().length]; // TODO
            BridgeContext context = seiModel.getBridgeContext();
            try {
                responseBuilder.readResponse(req,args,context);
            } catch (JAXBException e) {
                throw new DeserializationException("failed.to.read.response",e);
            } catch (XMLStreamException e) {
                throw new DeserializationException("failed.to.read.response",e);
            }
            try {
                Object ret = method.invoke(proxy, args);
            } catch(Exception e) {
                e.printStackTrace();
            }
            // TODO with return value
            return createResponseMessage(args);
        } finally {
            pool.recycle(m);
        }
    }

    /**
     * Creates a response {@link JAXBMessage} from method arguments.
     *
     */
    private Message createResponseMessage(Object[] args) {
        Message msg = bodyBuilder.createMessage(args);

        for (MessageFiller filler : inFillers)
            filler.fillIn(args,msg);

        return msg;
    }
}
