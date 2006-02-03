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
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.Provider;

/**
 * Keeps all the {@link Provider} endpoint information for SOAP binding. It also
 * does parameter binding({@link Provider}'s invoke() has only one parameter,
 * and one return value).
 *
 * @author Jitendra Kotamraju
 */
public class SOAPProviderEndpointModel extends ProviderEndpointModel {
    private SOAPVersion soapVersion;
    private Class implementorClass;
    private Service.Mode mode;
    private boolean isSource;

    public SOAPProviderEndpointModel(Class implementorClass, SOAPVersion soapVersion) {
        this.implementorClass = implementorClass;
        this.soapVersion = soapVersion;
    }

    /**
     * Finds parameter type, mode and throws an exception if Service.Mode and
     * parameter type combination is invalid.
     */
    @Override
    public void createModel() {
        isSource = isSource(implementorClass);
        boolean isSoapMessage = isSoapMessage(implementorClass);
        mode = getServiceMode(implementorClass);
        if (!(isSource || isSoapMessage)) {
            throw new IllegalArgumentException(
                "Endpoint should implement Provider<Source> or Provider<SOAPMessage>");
        }
        if (mode == Service.Mode.PAYLOAD && !isSource) {
            throw new IllegalArgumentException(
                "Illeagal combination Mode.PAYLOAD and Provider<SOAPMessage>");
        }
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
                responseMsg = Messages.create(source);
            } else {
                SOAPMessage soapMsg = (SOAPMessage)returnValue;
                responseMsg = Messages.create(soapMsg);
            }
        }
        return responseMsg;
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
