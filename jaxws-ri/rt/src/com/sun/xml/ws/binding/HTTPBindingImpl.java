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
package com.sun.xml.ws.binding;

import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.client.HandlerConfiguration;
import com.sun.xml.ws.resources.ClientMessages;

import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.http.HTTPBinding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author WS Development Team
 */
public class HTTPBindingImpl extends BindingImpl implements HTTPBinding {

    /**
     * Use {@link BindingImpl#create(BindingID)} to create this.
     */
    HTTPBindingImpl() {
        // TODO: implement a real encoder/decoder for these
        super(BindingID.XML_HTTP);
    }

    /**
     * This method separates the logical and protocol handlers and
     * sets the HandlerConfiguration.
     * Only logical handlers are allowed with HTTPBinding.
     * Setting SOAPHandlers throws WebServiceException
     */
    protected HandlerConfiguration createHandlerConfig(List<Handler> handlerChain) {
        List<LogicalHandler> logicalHandlers = new ArrayList<LogicalHandler>();
        for (Handler handler : handlerChain) {
            if (!(handler instanceof LogicalHandler)) {
                throw new WebServiceException(ClientMessages.NON_LOGICAL_HANDLER_SET(handler.getClass()));
            } else {
                logicalHandlers.add((LogicalHandler) handler);
            }
        }
        return new HandlerConfiguration(
                Collections.<String>emptySet(),
                Collections.<QName>emptySet(),
                handlerChain,logicalHandlers,null,null);
    }
}
