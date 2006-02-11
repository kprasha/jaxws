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
import com.sun.xml.ws.util.QNameMap;

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

    /**
     * For each method on the port interface we have
     * a {@link MethodHandler} that processes it.
     */
    private final QNameMap<EndpointMethodHandler> methodHandlers;
    private static final String EMPTY_PAYLOAD_LOCAL = "";
    private static final String EMPTY_PAYLOAD_NSURI = "";


    public SEIInvokerPipe(AbstractSEIModelImpl model,InstanceResolver instanceResolver, WSBinding binding) {
        this.model = model;
        methodHandlers = new QNameMap<EndpointMethodHandler>();
        // fill in methodHandlers.
        for( JavaMethodImpl m : model.getJavaMethods() ) {
            EndpointMethodHandler handler = new EndpointMethodHandler(model,m,binding, instanceResolver);
            QName payloadName = model.getQNameForJM(m);     // TODO need a new method on JavaMethodImpl
            methodHandlers.put(payloadName.getNamespaceURI(), payloadName.getLocalPart(), handler);
        }
    }

    /*
     * This binds the parameters for SEI endpoints and invokes the endpoint method. The
     * return value, and response Holder arguments are used to create a new {@link Message}
     * that traverses through the Pipeline to transport.
     */
    public Packet process(Packet req) {
        Message msg = req.getMessage();
        String localPart = msg.getPayloadLocalPart();
        String nsUri;
        if (localPart == null) {
            localPart = EMPTY_PAYLOAD_LOCAL;
            nsUri = EMPTY_PAYLOAD_NSURI;
        } else {
            nsUri = msg.getPayloadNamespaceURI();
        }
        EndpointMethodHandler handler = methodHandlers.get(nsUri, localPart);
        Packet res = handler.invoke(req);
        return res;

    }

    public Pipe copy(PipeCloner cloner) {
        cloner.add(this,this);
        return this;
    }
}
