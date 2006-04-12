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

package com.sun.xml.ws.client.dispatch;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.sandbox.message.impl.saaj.SAAJMessage;
import com.sun.xml.ws.sandbox.message.impl.source.PayloadSourceMessage;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
/**
 * TODO: Use sandbox classes, update javadoc
 */

/**
 * The <code>SourceDispatch</code> class provides support
 * for the dynamic invocation of a service endpoint operation using XML
 * constructs. The <code>javax.xml.ws.Service</code>
 * interface acts as a factory for the creation of <code>SourceDispatch</code>
 * instances.
 *
 * @author WS Development Team
 * @version 1.0
 */

public class SourceDispatch extends DispatchImpl<Source> {

    public SourceDispatch(QName port, Class<Source> aClass, Service.Mode mode, WSServiceDelegate owner, Pipe pipe, BindingImpl binding) {
        super(port, aClass, mode, owner, pipe, binding);
    }

    Source toReturnValue(Packet response) {
        Message msg = response.getMessage();

        switch (mode){
            case PAYLOAD:
                return msg.readPayloadAsSource();
            case MESSAGE:
                return msg.readEnvelopeAsSource();
            default:
                throw new WebServiceException("Unrecognized dispatch mode");
        }
    }

    Packet createPacket(Source msg) {

        final Message message;

        switch (mode) {
            case PAYLOAD:
                          //check on this for different binding
                message = (msg == null) ? Messages.createEmpty(soapVersion) : Messages.createUsingPayload(msg, soapVersion);
                //message = new PayloadSourceMessage(msg, soapVersion);
                break;
            case MESSAGE:
                //check on this for message mode
                if (isXMLHttp())
                    message = (msg == null) ? Messages.createEmpty(soapVersion) : Messages.createUsingPayload(msg, soapVersion);
                else {
                    //Todo: temporary until ProtocolSourceMessage is done
                    SOAPMessage soapmsg;
                    try {
                        //todo:
                        soapmsg = binding.getSOAPVersion().saajMessageFactory.createMessage();
                        soapmsg.getSOAPPart().setContent(msg);
                        soapmsg.saveChanges();
                    } catch (SOAPException e) {
                        throw new WebServiceException(e);
                    }
                    message = new SAAJMessage(soapmsg);
                }
                //todo:temp until ProtocolSourceMessage implemented
                //message = new ProtocolSourceMessage(msg);
                //todo: uncomment above when ProtocolSourceMessage is done
                break;
            default:
                throw new WebServiceException("Unrecognized message mode");
        }

        return new Packet(message);
    }
   
}
