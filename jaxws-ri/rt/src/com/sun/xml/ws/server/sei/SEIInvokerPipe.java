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

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.client.sei.MethodHandler;
import com.sun.xml.ws.encoding.soap.SOAP12Constants;
import com.sun.xml.ws.encoding.soap.SOAPConstants;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.sandbox.fault.SOAPFaultBuilder;
import com.sun.xml.ws.server.InvokerPipe;
import com.sun.xml.ws.util.QNameMap;

import javax.xml.namespace.QName;

/**
 * This pipe is used to invoke SEI based endpoints.
 *
 * @author Jitendra Kotamraju
 */
public class SEIInvokerPipe extends InvokerPipe {

    /**
     * For each method on the port interface we have
     * a {@link MethodHandler} that processes it.
     */
    private final QNameMap<EndpointMethodHandler> methodHandlers;
    private static final String EMPTY_PAYLOAD_LOCAL = "";
    private static final String EMPTY_PAYLOAD_NSURI = "";
    private final SOAPVersion soapVersion;

    public SEIInvokerPipe(AbstractSEIModelImpl model,InstanceResolver instanceResolver, WSBinding binding) {
        super(instanceResolver);
        this.soapVersion = binding.getSOAPVersion();
        methodHandlers = new QNameMap<EndpointMethodHandler>();
        // fill in methodHandlers.
        for( JavaMethodImpl m : model.getJavaMethods() ) {
            EndpointMethodHandler handler = new EndpointMethodHandler(this,model,m,binding);
            QName payloadName = model.getQNameForJM(m);     // TODO need a new method on JavaMethodImpl
            methodHandlers.put(payloadName.getNamespaceURI(), payloadName.getLocalPart(), handler);
        }
    }

    /**
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
        Packet res;
        if (handler == null) {
            // TODO optimize
            String faultString = "Cannot find dispatch method for "+
                    "{"+nsUri+"}"+localPart;
            QName faultCode = (soapVersion == SOAPVersion.SOAP_11)
                ? SOAPConstants.FAULT_CODE_CLIENT
                : SOAP12Constants.FAULT_CODE_CLIENT;
            Message faultMsg = SOAPFaultBuilder.createSOAPFaultMessage(
                    soapVersion, faultString, faultCode);
            res = new Packet(faultMsg);
        } else {
            res = handler.invoke(req);
        }
        res.invocationProperties.putAll(req.invocationProperties);

        return res;
    }
}
