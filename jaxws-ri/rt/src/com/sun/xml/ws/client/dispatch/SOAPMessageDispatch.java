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

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.message.saaj.SAAJMessage;

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
public class SOAPMessageDispatch extends com.sun.xml.ws.client.dispatch.DispatchImpl<SOAPMessage> {
    public SOAPMessageDispatch(QName port, Class<SOAPMessage> aClass, Service.Mode mode, WSServiceDelegate owner, Pipe pipe, BindingImpl binding) {
        super(port, aClass, mode, owner, pipe, binding);
    }

    Packet createPacket(SOAPMessage arg) {
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

        Packet msg = new Packet(new SAAJMessage(arg));
        msg.httpRequestHeaders = ch;
        return msg;
    }

    SOAPMessage toReturnValue(Packet response) {
        try {
            return response.getMessage().readAsSOAPMessage();
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }
}
