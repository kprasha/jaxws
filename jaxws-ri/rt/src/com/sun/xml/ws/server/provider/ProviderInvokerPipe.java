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
package com.sun.xml.ws.server.provider;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractPipeImpl;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.server.InstanceResolver;

import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * This pipe is used to invoke the {@link Provider} endpoints.
 *
 * @author Jitendra Kotamraju
 */
public class ProviderInvokerPipe extends AbstractPipeImpl {

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.ProviderInvokerPipe");
    private final ProviderEndpointModel model;
    private final InstanceResolver<? extends Provider> instanceResolver;

    private static final Method invoke_Method;
    static {
        try {
            Class[] methodParams = { Object.class };
            invoke_Method = (Provider.class).getMethod("invoke", methodParams);
        } catch (NoSuchMethodException e) {
            throw new WebServiceException(e.getMessage(), e);
        }
    }
    
    public ProviderInvokerPipe(Class<? extends Provider> implType, InstanceResolver<? extends Provider> instanceResolver, WSBinding binding) {
        this.instanceResolver = instanceResolver;

        SOAPVersion soapVersion = binding.getSOAPVersion();
        if (soapVersion != null) {
            model = new SOAPProviderEndpointModel(implType,soapVersion);
        } else {
            model = new XMLProviderEndpointModel(implType);
        }
        model.createModel();
    }

    /*
     * This binds the parameter for Provider endpoints and invokes the
     * invoke() method of {@linke Provider} endpoint. The return value from
     * invoke() is used to create a new {@link Message} that traverses
     * through the Pipeline to transport.
     */
    public Packet process(Packet request) {
        Object parameter = model.getParameter(request.getMessage());
        Provider servant = instanceResolver.resolve(request);
        logger.fine("Invoking Provider Endpoint "+servant);
        Object returnValue = servant.invoke(parameter);

        Packet response;
        if(returnValue==null)
            response = new Packet(null);
        else
            response = new Packet(model.getResponseMessage(returnValue));

        // KK - see javadoc of isOneWay. It's only for clients, so shouldn't be used on the server
        //if (returnValue == null) {
        //    response.isOneWay = true;
        //}
        return response;
    }

    public Pipe copy(PipeCloner cloner) {
        cloner.add(this,this);
        return this;
    }

}
