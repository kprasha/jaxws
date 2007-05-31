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
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.client.dispatch;

import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.encoding.xml.XMLMessage;
import com.sun.xml.ws.message.source.PayloadSourceMessage;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service.Mode;
import java.io.IOException;

/**
 * {@link Dispatch} implementation for {@link Source} and XML/HTTP binding.
 *
 * @author Kathy Walsh
 * @author Kohsuke Kawaguchi
 * @see SOAPSourceDispatch
 */
final class RESTSourceDispatch extends DispatchImpl<Source> {

    public RESTSourceDispatch(QName port, Mode mode, WSServiceDelegate owner, Tube pipe, BindingImpl binding, WSEndpointReference epr) {
        super(port, mode, owner, pipe, binding, epr);
        assert isXMLHttp(binding);
    }

    @Override
    Source toReturnValue(Packet response) {
        Message msg = response.getMessage();
        try {
            return new StreamSource(XMLMessage.getDataSource(msg).getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    Packet createPacket(Source msg) {
        Message message;

        if(msg==null)
            message = Messages.createEmpty(soapVersion);
        else
            message = new PayloadSourceMessage(null, msg, setOutboundAttachments(), soapVersion);

        return new Packet(message);
    }
}
