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

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import java.lang.reflect.Method;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceException;
import java.util.logging.Logger;
import com.sun.xml.ws.server.RuntimeEndpointInfo;

/**
 * This pipe is used to invoke the Provider endpoints. 
 *
 * @author Jitendra Kotamraju
 */
public class ProviderInvokerPipe implements Pipe {

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.ProviderInvokerPipe");
    private final RuntimeEndpointInfo endpointInfo;

    public static final Method invoke_Method;
    static {
        try {
            Class[] methodParams = { Object.class };
            invoke_Method = (Provider.class).getMethod("invoke", methodParams);
        } catch (NoSuchMethodException e) {
            throw new WebServiceException(e.getMessage(), e);
        }
    };
    
    public ProviderInvokerPipe(RuntimeEndpointInfo endpointInfo) {
        this.endpointInfo = endpointInfo;
    }

    /*
     * This binds the parameter for Provider endpoints and invokes the
     * invoke() method of Provider endpoint. The return value from invoke()
     * is used to create a new Message that traverses through the Pipeline to
     * transport.
     */
    public Message process(Message msg) {
        // TODO : some of the information can be moved to ProviderModel and that
        // can be accessed from RuntimeEndpointInfo. Finding Source or SOAPMessage
        // needs to be done only once so that it is not repeated for every
        // request
        
        // TODO : Take care of XML/HTTP binding
        
        // TODO : Take care of SOAP 1.2 binding

        Class providerClass = endpointInfo.getImplementorClass();
        boolean isSource = isSource(providerClass);
        boolean isSoapMessage = isSoapMessage(providerClass);
        Service.Mode mode = getServiceMode(providerClass);
        if (!(isSource || isSoapMessage)) {
            throw new UnsupportedOperationException(
                    "Endpoint should implement Provider<Source> or Provider<SOAPMessage>");
        }
        Object[] data = new Object[1];
        if (mode == Service.Mode.PAYLOAD) {
            if (isSource) {
                data[0] = msg.readPayloadAsSource();
            } else {
                throw new UnsupportedOperationException(
                        "Illeagal combination Mode.PAYLOAD and Provider<SOAPMessage>");
            }
        } else {
            if (isSource) {
                // Get SOAPMessage's SOAPPart as Source
                data[0]= msg.readEnvelopeAsSource();
            } else {
                try {
                    data[0] = msg.readAsSOAPMessage();
                } catch(SOAPException se) {
                    throw new WebServiceException(se);
                }
            }
        }
        Provider servant = (Provider)endpointInfo.getImplementor();
        Message responseMsg = null;
        Object response = servant.invoke(data[0]);
        if (mode == Service.Mode.PAYLOAD) {
            Source source = (Source)response;
            // TODO : to take care of SOAP 1.2
            responseMsg = Messages.createUsingPayload(source, SOAPVersion.SOAP_11);
        } else {
            if (isSource) {
                Source source = (Source)response;
                responseMsg = Messages.create(source);
            } else {
                SOAPMessage soapMsg = (SOAPMessage)response;
                responseMsg = Messages.create(soapMsg);
            }
        }
        if (response == null) {
            responseMsg.getProperties().isOneWay = true;
        }
        return responseMsg;
    }

    public void preDestroy() {
    }

    public Pipe copy(PipeCloner cloner) {
        return this;
    }
    
    /*
     * Is it PAYLOAD or MESSAGE ??
     */
    private static Service.Mode getServiceMode(Class c) {
        ServiceMode mode = (ServiceMode)c.getAnnotation(ServiceMode.class);
        if (mode == null) {
            return Service.Mode.PAYLOAD;
        }
        return mode.value();
    }

    /*
     * Is it Provider<Source> ?
     */
    private static boolean isSource(Class c) {
        try {
            c.getMethod("invoke",  Source.class);
            return true;
        } catch(NoSuchMethodException ne) {
            // ignoring intentionally
        }
        return false;
    }

    /*
     * Is it Provider<SOAPMessage> ?
     */
    private static boolean isSoapMessage(Class c) {
        try {
            c.getMethod("invoke",  SOAPMessage.class);
            return true;
        } catch(NoSuchMethodException ne) {
            // ignoring intentionally
        }
        return false;
    }

}
