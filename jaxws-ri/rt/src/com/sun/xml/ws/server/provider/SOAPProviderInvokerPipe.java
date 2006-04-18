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

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.fault.SOAPFaultBuilder;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

/**
 * This pipe is used to invoke SOAP/HTTP {@link Provider} endpoints.
 *
 * @author Jitendra Kotamraju
 */
public class SOAPProviderInvokerPipe extends ProviderInvokerPipe {
    
    private final boolean isSource;
    private final Service.Mode mode;
    private final SOAPVersion soapVersion;
    
    public SOAPProviderInvokerPipe(InstanceResolver<? extends Provider> instanceResolver, ProviderEndpointModel model, SOAPVersion soapVersion) {
        super(instanceResolver);
        this.isSource = model.isSource();
        this.mode = model.getServiceMode();
        this.soapVersion = soapVersion;
    }
         
    /**
     * {@link Message} is converted to correct parameter for invoke() method
     */
    @Override
    public Object getParameter(Message msg) {
        Object parameter = null;
        if (mode == Service.Mode.PAYLOAD) {
            if (isSource) {
                parameter = msg.readPayloadAsSource();
            }
            // else doesn't happen because ProviderModel takes care of it
        } else {
            if (isSource) {
                // Get SOAPMessage's envelope as Source
                parameter = msg.readEnvelopeAsSource();
            } else {
                try {
                    parameter = msg.readAsSOAPMessage();
                } catch(SOAPException se) {
                    throw new WebServiceException(se);
                }
            }
        }
        return parameter;
    }
    
    @Override
    public Message getResponseMessage(Exception e) {
        return SOAPFaultBuilder.createSOAPFaultMessage(soapVersion, null, e);
    }

    /**
     * return value of invoke() is converted to {@link Message}
     */
    @Override
    public Message getResponseMessage(Object returnValue) {
        Message responseMsg;
        if (mode == Service.Mode.PAYLOAD) {
            Source source = (Source)returnValue;
            responseMsg = Messages.createUsingPayload(source, soapVersion);
        } else {
            if (isSource) {
                Source source = (Source)returnValue;
                responseMsg = Messages.create(source, soapVersion);
            } else {
                SOAPMessage soapMsg = (SOAPMessage)returnValue;
                responseMsg = Messages.create(soapMsg);
            }
        }
        return responseMsg;
    }
}