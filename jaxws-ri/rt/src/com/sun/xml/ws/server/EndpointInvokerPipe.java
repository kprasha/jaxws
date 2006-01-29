/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.ws.server;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.MessageProperties;
import com.sun.xml.ws.api.model.soap.SOAPBinding;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.client.RequestContext;
import com.sun.xml.ws.client.port.MethodHandler;
import com.sun.xml.ws.client.port.PortInterfaceStub;
import com.sun.xml.ws.encoding.soap.DeserializationException;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.model.ParameterImpl;
import com.sun.xml.ws.model.WrapperParameter;
import com.sun.xml.ws.util.Pool;
import com.sun.xml.ws.util.Pool.BridgeContext;
import com.sun.xml.ws.util.Pool.Marshaller;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;

/**
 * This pipe is used to invoke SEI based endpoints. 
 *
 * @author Jitendra Kotamraju
 */
public class EndpointInvokerPipe implements Pipe {

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.EndpointInvokerPipe");
    private final RuntimeEndpointInfo endpointInfo;
    /**
     * For each method on the port interface we have
     * a {@link MethodHandler} that processes it.
     */
    private final Map<Method,MethodHandler> methodHandlers = new HashMap<Method,MethodHandler>();

    /**
     * JAXB marshaller pool.
     *
     * TODO: this pool can be shared across {@link Stub}s.
     */
    public final Pool.Marshaller marshallers;

    // TODO: ditto
    public final Pool.BridgeContext bridgeContexts;
    
    public EndpointInvokerPipe(RuntimeEndpointInfo endpointInfo) {
        this.endpointInfo = endpointInfo;

        AbstractSEIModelImpl seiModel = endpointInfo.getRuntimeModel();
        this.marshallers = new Pool.Marshaller(seiModel.getJAXBContext());
        this.bridgeContexts = new Pool.BridgeContext(seiModel.getJAXBContext());
        /*
        Map<WSDLBoundOperation,SyncMethodHandler> syncs = new HashMap<WSDLBoundOperation, SyncMethodHandler>();

        // fill in methodHandlers.
        // first fill in sychronized versions
        for( JavaMethodImpl m : seiModel.getJavaMethods() ) {
            SyncMethodHandler handler = new SyncMethodHandler(m);
            syncs.put(m.getOperation(),handler);
            methodHandlers.put(m.getMethod(),handler);
        }
         */
    }

    /*
     * This binds the parameter for Provider endpoints and invokes the
     * invoke() method of {@linke Provider} endpoint. The return value from
     * invoke() is used to create a new {@link Message} that traverses
     * through the Pipeline to transport.
     */
    public Message process(Message msg) {
        // TODO need find dispatch method using different mechanisms
        // TODO need to define an API
        
        String localPart = msg.getPayloadLocalPart();
        String nsURI = msg.getPayloadNamespaceURI();
        QName opName = new QName(nsURI, localPart);
        
        AbstractSEIModelImpl seiModel = endpointInfo.getRuntimeModel();
        JavaMethodImpl javaMethod = seiModel.getJavaMethod(opName);
        
        Object[] args = null;
        Object servant = endpointInfo.getImplementor();
        logger.fine("Invoking SEI based Endpoint "+servant);
        Method method = javaMethod.getMethod();
        try {
            Object returnValue = method.invoke(servant, args);
        } catch (IllegalArgumentException e) {
            throw new WebServiceException(e);
        } catch (IllegalAccessException e) {
            throw new WebServiceException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                if (!(cause instanceof RuntimeException) && cause instanceof Exception ) {
                    // Service specific exception
                } else {
                    throw new WebServiceException(e);
                }
            } else {
                throw new WebServiceException(e);
            }
        }
        Message response = null;
        return response;
    }

    public void preDestroy() {
    }

    public Pipe copy(PipeCloner cloner) {
        return this;
    }
    


}
