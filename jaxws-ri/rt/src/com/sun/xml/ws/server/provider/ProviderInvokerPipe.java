/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.xml.ws.server.provider;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.api.server.Invoker;
import com.sun.xml.ws.server.InvokerPipe;

import javax.xml.ws.Provider;
import java.util.logging.Logger;

/**
 * This pipe is used to invoke the {@link Provider} endpoints.
 *
 * @author Jitendra Kotamraju
 */
public abstract class ProviderInvokerPipe<T> extends InvokerPipe<Provider<T>> {

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.ProviderInvokerPipe");
    
    protected Parameter<T> parameter;
    protected Response<T> response;

    public ProviderInvokerPipe(Invoker invoker) {
        super(invoker);
    }

    /*
     * This binds the parameter for Provider endpoints and invokes the
     * invoke() method of {@linke Provider} endpoint. The return value from
     * invoke() is used to create a new {@link Message} that traverses
     * through the Pipeline to transport.
     */
    public Packet process(Packet request) {
        T param = parameter.getParameter(request.getMessage());

        logger.fine("Invoking Provider Endpoint");

        T returnValue;
        try {
            returnValue = getInvoker(request).invokeProvider(request, param);
        } catch(Exception e) {
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
            return request.createResponse(response.getResponse(returnValue));
        }
    }
    
    /**
     * Binds {@link Message} to method invocation parameter
     */
    static interface Parameter<T> {
        T getParameter(Message msg);
    }
    
    /**
     * Creates {@link Message} from method invocation's return value
     */
    static interface Response<T> {
        Message getResponse(T returnValue);
    }
     
    /**
     * Creates a fault {@link Message} from method invocation's exception
     */
    protected abstract Message getResponseMessage(Exception e);
}
