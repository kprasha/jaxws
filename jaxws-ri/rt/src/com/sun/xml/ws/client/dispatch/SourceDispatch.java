/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */

package com.sun.xml.ws.client.dispatch;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
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
                //todo: uncomment when Pa
                //return
                return msg.readEnvelopeAsSource();
            default:
                throw new WebServiceException("Unrecognized dispatch mode");
        }
    }

    Packet createPacket(Source msg) {
        Message message;
        switch (mode) {
            case PAYLOAD:
                message = new PayloadSourceMessage(msg, soapVersion);
                break;
            case MESSAGE:
                //Todo: temporary until ProtocolSourceMessage is done
                SOAPMessage soapmsg;
                try {
                    //todo:
                    soapmsg = binding.getSOAPVersion().saajFactory.createMessage();
                    soapmsg.getSOAPPart().setContent(msg);
                    soapmsg.saveChanges();
                } catch (SOAPException e) {
                    throw new WebServiceException(e);
                }
                message = new SAAJMessage(soapmsg);
                //todo:temp until ProtocolSourceMessage implemented
                //message = new ProtocolSourceMessage(msg);
                //todo: uncomment above when ProtocolSourceMessage is done
                break;
            default:
                throw new WebServiceException("Unrecognized message mode");
        }

        // KK - stubs can't assume what form messages are serialized.
        // don't assume that it's even going to be formated as text/xml.

        //Map<String, List<String>> ch = new HashMap<String, List<String>>();
        //
        //List<String> ct = new ArrayList<String>();
        //ct.add("text/xml");
        //ch.put("Content-Type", ct);
        //
        //List<String> cte = new ArrayList<String>();
        //cte.add("binary");
        //ch.put("Content-Transfer-Encoding", cte);
        //
        //Packet p = new Packet(message);
        //p.httpRequestHeaders = ch;

        return new Packet(message);
    }
}