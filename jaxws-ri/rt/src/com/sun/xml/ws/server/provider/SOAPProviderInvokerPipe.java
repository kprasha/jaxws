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
    
    private final SOAPVersion soapVersion;
    
    public SOAPProviderInvokerPipe(InstanceResolver<? extends Provider> instanceResolver, ProviderEndpointModel model, SOAPVersion soapVersion) {
        super(instanceResolver);
        this.soapVersion = soapVersion;

        if (model.getServiceMode() == Service.Mode.PAYLOAD) {
            parameter = new PayloadSourceParameter();
        } else {
            parameter = model.isSource() ? new MessageSourceParameter() : new SOAPMessageParameter();
        }
        
        if (model.getServiceMode() == Service.Mode.PAYLOAD) {
            response = new PayloadSourceResponse();
        } else {
            response = model.isSource() ? new MessageSourceResponse() : new SOAPMessageResponse();
        }
    }
    
    private static final class PayloadSourceParameter implements Parameter<Source> {
        public Source getParameter(Message msg) {
            return msg.readPayloadAsSource();
        }
    }
    
    private static final class MessageSourceParameter implements Parameter<Source> {
        public Source getParameter(Message msg) {
            return msg.readEnvelopeAsSource();
        }
    }
    
    private static final class SOAPMessageParameter implements Parameter<SOAPMessage> {
        public SOAPMessage getParameter(Message msg) {
            try {
                return msg.readAsSOAPMessage();
            } catch(SOAPException se) {
                throw new WebServiceException(se);
            }
        }
    }
    
    @Override
    protected Message getResponseMessage(Exception e) {
        return SOAPFaultBuilder.createSOAPFaultMessage(soapVersion, null, e);
    }
    
    private final class PayloadSourceResponse implements Response<Source> {
        public Message getResponse(Source source) {
            return Messages.createUsingPayload(source, soapVersion);
        }
    }
    
    private final class MessageSourceResponse implements Response<Source> {
        public Message getResponse(Source source) {
            return Messages.create(source, soapVersion);
        }
    }
    
    private static final class SOAPMessageResponse implements Response<SOAPMessage> {
        public Message getResponse(SOAPMessage soapMsg) {
            return Messages.create(soapMsg);
        }
    }
}