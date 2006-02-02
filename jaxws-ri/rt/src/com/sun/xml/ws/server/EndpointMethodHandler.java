package com.sun.xml.ws.server;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.encoding.soap.DeserializationException;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.model.ParameterImpl;
import com.sun.xml.ws.model.WrapperParameter;
import com.sun.xml.ws.sandbox.message.impl.jaxb.JAXBMessage;
import com.sun.xml.ws.util.Pool;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.Holder;
import java.util.ArrayList;
import java.util.List;
import javax.jws.WebParam.Mode;

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
 * At the construction time, we prepare {@link EndpointResponseMessageBuilder} and {@link MessageFiller}s
 * that know how to move arguments into a {@link Message}.
 * Some arguments go to the payload, some go to headers, still others go to attachments.
 *
 * @author Jitendra Kotamraju
 */
public final class EndpointMethodHandler {

    // these objects together create a message from method parameters
    private final EndpointResponseMessageBuilder bodyBuilder;
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

    private final EndpointArgumentsBuilder responseBuilder;

    public EndpointMethodHandler(RuntimeEndpointInfo endpointInfo, JavaMethodImpl method) {

        this.seiModel = endpointInfo.getRuntimeModel();
        soapVersion = endpointInfo.getBinding().getSOAPVersion();
        this.method = method.getMethod();

        {// prepare objects for creating messages
            List<ParameterImpl> rp = method.getResponseParameters();

            EndpointResponseMessageBuilder bodyBuilder = null;
            List<MessageFiller> fillers = new ArrayList<MessageFiller>();
            valueGetters = new ValueGetter[rp.size()];

            for (ParameterImpl param : rp) {
                ValueGetter getter = ValueGetter.get(param);

                switch(param.getOutBinding().kind) {
                case BODY:
                    if(param.isWrapperStyle()) {
                        if(param.getParent().getBinding().isRpcLit())
                            bodyBuilder = new EndpointResponseMessageBuilder.RpcLit((WrapperParameter)param, seiModel, soapVersion);
                        else
                            bodyBuilder = new EndpointResponseMessageBuilder.DocLit((WrapperParameter)param, seiModel, soapVersion);
                    } else {
                        bodyBuilder = new EndpointResponseMessageBuilder.Bare(param, seiModel, soapVersion);
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
                    bodyBuilder = EndpointResponseMessageBuilder.EMPTY_SOAP11;
                    break;
                case SOAP_12:
                    bodyBuilder = EndpointResponseMessageBuilder.EMPTY_SOAP12;
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
            List<EndpointArgumentsBuilder> builders = new ArrayList<EndpointArgumentsBuilder>();

            for( ParameterImpl param : rp ) {
                EndpointValueSetter setter = EndpointValueSetter.get(param);
                switch(param.getInBinding().kind) {
                case BODY:
                    if(param.isWrapperStyle()) {
                        if(param.getParent().getBinding().isRpcLit())
                            builders.add(new EndpointArgumentsBuilder.RpcLit((WrapperParameter)param));
                        else
                            builders.add(new EndpointArgumentsBuilder.DocLit((WrapperParameter)param, Mode.OUT));
                    } else {
                        builders.add(new EndpointArgumentsBuilder.Body(param.getBridge(),setter));
                    }
                    break;
                case HEADER:
                    builders.add(new EndpointArgumentsBuilder.Header(param, setter));
                    break;
                case ATTACHMENT:
                    // TODO: implement this later
                    throw new UnsupportedOperationException();
                case UNBOUND:
                    builders.add(new EndpointArgumentsBuilder.NullSetter(setter,
                        EndpointArgumentsBuilder.getVMUninitializedValue(param.getTypeReference().type)));
                    break;
                default:
                    throw new AssertionError();
                }
            }

            switch(builders.size()) {
            case 0:
                responseBuilder = EndpointArgumentsBuilder.NONE;
                break;
            case 1:
                responseBuilder = builders.get(0);
                break;
            default:
                responseBuilder = new EndpointArgumentsBuilder.Composite(builders);
            }
        }

        this.isOneWay = method.getMEP().isOneWay();
    }

    public Packet invoke(Object proxy, Message req) {

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
            Object ret = null;
            Message responseMessage = null;
            try {
                ret = method.invoke(proxy, args);
                responseMessage = isOneWay ? null : createResponseMessage(args, ret);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause != null && !(cause instanceof RuntimeException) && cause instanceof Exception) {
                    // Service specific exception
                    responseMessage = createFaultMessageForServiceException(e);
                } else {
                    responseMessage = createFaultMessageForOtherException(e);
                }
            } catch (Exception e) {
                responseMessage = createFaultMessageForOtherException(e);
            }
            Packet respPacket = new Packet(responseMessage);
            respPacket.isOneWay = isOneWay;
            return respPacket;
        } finally {
            pool.recycle(m);
        }
    }

    /**
     * Creates a response {@link JAXBMessage} from method arguments.
     *
     */
    private Message createResponseMessage(Object[] args, Object returnValue) {
        Message msg = bodyBuilder.createMessage(args, returnValue);

        for (MessageFiller filler : inFillers)
            filler.fillIn(args, returnValue, msg);

        return msg;
    }
    
    /**
     * Creates a response {@link JAXBMessage} from method arguments.
     *
     */
    private Message createFaultMessageForServiceException(Exception e) {
 
        return null;
    }
    
    private Message createFaultMessageForOtherException(Exception e) {

        return null;
    }
}
