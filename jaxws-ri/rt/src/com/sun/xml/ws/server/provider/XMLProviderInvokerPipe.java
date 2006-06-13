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
import com.sun.xml.ws.api.server.Invoker;
import com.sun.xml.ws.encoding.xml.XMLMessage;
import javax.activation.DataSource;
import javax.xml.transform.Source;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import com.sun.xml.ws.encoding.xml.XMLMessage.HasDataSource;

/**
 * This pipe is used to invoke XML/HTTP {@link Provider} endpoints.
 *
 * @author Jitendra Kotamraju
 */
public class XMLProviderInvokerPipe extends ProviderInvokerPipe {
     
    public XMLProviderInvokerPipe(Invoker invoker, ProviderEndpointModel model) {
        super(invoker);
        
        if (model.getServiceMode() == Service.Mode.PAYLOAD) {
            parameter = new PayloadSourceParameter();
        } else {
            parameter = model.isSource() ? new PayloadSourceParameter() : new DataSourceParameter();
        }
        
        if (model.getServiceMode() == Service.Mode.PAYLOAD) {
            response = new PayloadSourceResponse();
        } else {
            response = model.isSource() ? new PayloadSourceResponse() : new DataSourceResponse();
        }
    }
    
    private static final class PayloadSourceParameter implements Parameter<Source> {
        public Source getParameter(Message msg) {
            return msg.readPayloadAsSource();
        }
    }
    
    private static final class DataSourceParameter implements Parameter<DataSource> {
        public DataSource getParameter(Message msg) {
            return (msg instanceof HasDataSource)
                ? ((HasDataSource)msg).getDataSource()
                : XMLMessage.getDataSource(msg);
        }
    }
    
    @Override
    protected Message getResponseMessage(Exception e) {
        return null;    // TODO create a fault message
    }
    
    private static final class PayloadSourceResponse implements Response<Source> {
        public Message getResponse(Source source) {
            return Messages.createUsingPayload(source, SOAPVersion.SOAP_11);
        }
    }
    
    private static final class DataSourceResponse implements Response<DataSource> {
        public Message getResponse(DataSource ds) {
            return XMLMessage.create(ds);
        }
    }
}