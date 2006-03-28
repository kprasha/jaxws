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
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractPipeImpl;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.sandbox.fault.SOAPFaultBuilder;

import javax.xml.ws.Provider;
import java.util.logging.Logger;

/**
 * This pipe is used to invoke the {@link Provider} endpoints.
 *
 * @author Jitendra Kotamraju
 */
public abstract class ProviderInvokerPipe extends AbstractPipeImpl {

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.ProviderInvokerPipe");
    private final InstanceResolver<? extends Provider> instanceResolver;

    public ProviderInvokerPipe(InstanceResolver<? extends Provider> instanceResolver) {
        this.instanceResolver = instanceResolver;
    }

    /*
     * This binds the parameter for Provider endpoints and invokes the
     * invoke() method of {@linke Provider} endpoint. The return value from
     * invoke() is used to create a new {@link Message} that traverses
     * through the Pipeline to transport.
     */
    public Packet process(Packet request) {
        Object parameter = getParameter(request.getMessage());
        Provider servant = instanceResolver.resolve(request);
        logger.fine("Invoking Provider Endpoint "+servant);
        Object returnValue = null;
        try {
            returnValue = servant.invoke(parameter);
        } catch(RuntimeException e) {
            e.printStackTrace();
            Message responseMessage = getResponseMessage(e);
            return request.createResponse(responseMessage);
        }
        if (returnValue == null) {
            // Oneway. Send response code immediately for transports(like HTTP)
            // Don't do this above, since close() may generate some exceptions
            if (request.transportBackChannel != null) {
                request.transportBackChannel.close();
            }
            return request.createResponse(null);
        } else {
            return request.createResponse(getResponseMessage(returnValue));
        }
    }
    
    public abstract Object getParameter(Message msg);
    public abstract Message getResponseMessage(Object returnValue);
    public abstract Message getResponseMessage(Exception e);

    public Pipe copy(PipeCloner cloner) {
        cloner.add(this,this);
        return this;
    }

}
