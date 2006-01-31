/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */

package com.sun.xml.ws.client.dispatch;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.client.dispatch.DispatchImpl;
import com.sun.xml.ws.sandbox.message.impl.saaj.SAAJMessage;
import com.sun.xml.ws.sandbox.message.impl.source.PayloadSourceMessage;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    Source toReturnValue(Message response) {
        switch (mode){
            case PAYLOAD:
                return response.readPayloadAsSource();
            case MESSAGE:
                return response.readEnvelopeAsSource();
            default:
                throw new WebServiceException("Unrecognized dispatch mode");
        }
    }

    Message createMessage(Source msg) {
        Message message;
        switch (mode) {
            case PAYLOAD:
                message = new PayloadSourceMessage(msg, soapVersion);
                break;
            case MESSAGE:
                //Todo: temporary until protocol message is done
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
                break;
            default:
                throw new WebServiceException("Unrecognized message mode");
        }

        Map<String, List<String>> ch = new HashMap<String, List<String>>();

        List<String> ct = new ArrayList<String>();
        ct.add("text/xml");
        ch.put("Content-Type", ct);

        List<String> cte = new ArrayList<String>();
        cte.add("binary");
        ch.put("Content-Transfer-Encoding", cte);

        message.getProperties().httpRequestHeaders = ch;

        return message;
    }
}