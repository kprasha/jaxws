/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */

package com.sun.xml.ws.client.dispatch.rearch.soapmsg;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.client.dispatch.rearch.DispatchImpl;
import com.sun.xml.ws.sandbox.message.impl.saaj.SAAJMessage;

import javax.xml.namespace.QName;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/**
 * TODO: Use sandbox classes, update javadoc
 */

/**
 * The <code>SOAPMessageDispatch</code> class provides support
 * for the dynamic invocation of a service endpoint operation using
 * the <code>SOAPMessage</code> class. The <code>javax.xml.ws.Service</code>
 * interface acts as a factory for the creation of <code>SOAPMessageDispatch</code>
 * instances.
 *
 * @author WS Development Team
 * @version 1.0
 */
public class SOAPMessageDispatch extends DispatchImpl<SOAPMessage> {
    public SOAPMessageDispatch(QName port, Class<SOAPMessage> aClass, Service.Mode mode, WSServiceDelegate owner, Pipe pipe, BindingImpl binding) {
        super(port, aClass, mode, owner, pipe, binding);
    }

    protected Message createMessage(SOAPMessage arg) {
        MimeHeaders mhs = arg.getMimeHeaders();
        mhs.addHeader("Content-Type", "text/xml");
        mhs.addHeader("Content-Transfer-Encoding", "binary");
        Map<String, List<String>> ch = new HashMap<String, List<String>>();
        for (Iterator iter = arg.getMimeHeaders().getAllHeaders(); iter.hasNext();)
        {
            List<String> h = new ArrayList<String>();
            MimeHeader mh = (MimeHeader) iter.next();

            h.clear();
            h.add(mh.getValue());
            ch.put(mh.getName(), h);
        }

        Message msg = new SAAJMessage(arg);
        msg.getProperties().httpRequestHeaders = ch;
        return msg;
    }

    protected SOAPMessage toReturnValue(Message response) {
        try {
            return response.readAsSOAPMessage();
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }
}