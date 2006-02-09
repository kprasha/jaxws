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
package com.sun.xml.ws.server.sei;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractPipeImpl;
import com.sun.xml.ws.client.sei.MethodHandler;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.api.server.InstanceResolver;

import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This pipe is used to invoke SEI based endpoints. 
 *
 * @author Jitendra Kotamraju
 */
public class SEIInvokerPipe extends AbstractPipeImpl {

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.SEIInvokerPipe");
    private final AbstractSEIModelImpl model;
    private final InstanceResolver instanceResolver;
    /**
     * For each method on the port interface we have
     * a {@link MethodHandler} that processes it.
     */
    private final Map<Method, EndpointMethodHandler> methodHandlers = new HashMap<Method, EndpointMethodHandler>();
    private static final QName EMPTY_QNAME = new QName("");

    public SEIInvokerPipe(AbstractSEIModelImpl model,InstanceResolver instanceResolver, WSBinding binding) {
        this.model = model;
        this.instanceResolver = instanceResolver;

        // fill in methodHandlers.
        // first fill in sychronized versions
        for( JavaMethodImpl m : model.getJavaMethods() ) {
            EndpointMethodHandler handler = new EndpointMethodHandler(model,m,binding);
            methodHandlers.put(m.getMethod(),handler);
        }
    }

    /*
     * This binds the parameter for Provider endpoints and invokes the
     * invoke() method of {@linke Provider} endpoint. The return value from
     * invoke() is used to create a new {@link Message} that traverses
     * through the Pipeline to transport.
     */
    public Packet process(Packet req) {
        // TODO need find dispatch method using different mechanisms
        // TODO need to define an API

        Message msg = req.getMessage();

        String localPart = msg.getPayloadLocalPart();
        QName opName = (localPart == null)
            ? EMPTY_QNAME
            : new QName(msg.getPayloadNamespaceURI(), localPart);

        // TODO: Don't go through 2 map look up. Prepare a map
        // so that you can go from opName to handler just by one look-up. - KK
        JavaMethodImpl javaMethod = model.getJavaMethod(opName);
        Method method = javaMethod.getMethod();

        EndpointMethodHandler handler = methodHandlers.get(method);
        Object servant = instanceResolver.resolve(req);
        Packet res = handler.invoke(servant, msg);
        res.invocationProperties.putAll(req.invocationProperties);

        return res;
        // TODO: some properties need to be copied from request packet to the response packet
    }

    public Pipe copy(PipeCloner cloner) {
        cloner.add(this,this);
        return this;
    }
}
