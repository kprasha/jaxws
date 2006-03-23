/*
 * SOAPHandlerProcessor.java
 *
 * Created on February 8, 2006, 5:43 PM
 *
 *
 */

package com.sun.xml.ws.handler;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.encoding.soap.SOAP12Constants;
import com.sun.xml.ws.encoding.soap.SOAPConstants;
import com.sun.xml.ws.encoding.soap.streaming.SOAP12NamespaceConstants;
import java.util.List;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;

/**
 *
 * @author WS Development Team
 */
public class SOAPHandlerProcessor<C extends MessageUpdatableContext> extends HandlerProcessor<C> {
    
    /**
     * Creates a new instance of SOAPHandlerProcessor
     */
    public SOAPHandlerProcessor(WSBinding binding, List<? extends Handler> chain, boolean isClient) {
        super(binding,chain,isClient);
    }
    
    /**
     * Replace the message in the given message context with a
     * fault message. If the context already contains a fault
     * message, then return without changing it.
     */
    void insertFaultMessage(C context,
        ProtocolException exception) {
        try {
            if(!context.getPacketMessage().isFault()) {
                Message faultMessage;
                if (exception instanceof SOAPFaultException) {
                    faultMessage = Messages.create(((SOAPFaultException)exception).getFault());
                } else {
                    faultMessage = Messages.create(exception,binding.getSOAPVersion());
                }
                context.setPacketMessage(faultMessage);
            }
        } catch (Exception e) {
            // severe since this is from runtime and not handler
            logger.log(Level.SEVERE,
                "exception while creating fault message in handler chain", e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * <p>Figure out if the fault code local part is client,
     * server, sender, receiver, etc. This is called by
     * insertFaultMessage.
     *
     * <p>This method should only be called during a request,
     * because during a response an exception from a handler
     * is dispatched rather than replacing the message with
     * a fault. So this method can use the MESSAGE_OUTBOUND_PROPERTY
     * to determine whether it is being called on the client
     * or the server side. If this changes in the spec, then
     * something else will need to be passed to the method
     * to determine whether the fault code is client or server.
     *
     * <p>For determining soap version, start checking with the
     * latest version and default to soap 1.1.
     */
    private QName determineFaultCode(SOAPMessageContext context)
        throws SOAPException {

        SOAPEnvelope envelope =
            context.getMessage().getSOAPPart().getEnvelope();
        String uri = envelope.getNamespaceURI();

        
        if (isClient) {
            // client case
            if (uri.equals(SOAP12NamespaceConstants.ENVELOPE)) {
                return SOAP12Constants.FAULT_CODE_CLIENT;
            }
            return SOAPConstants.FAULT_CODE_CLIENT;
        } else {            
            //server case
            if (uri.equals(SOAP12NamespaceConstants.ENVELOPE)) {
                return SOAP12Constants.FAULT_CODE_SERVER;
            }
            return SOAPConstants.FAULT_CODE_SERVER;
        }    
    }

}
