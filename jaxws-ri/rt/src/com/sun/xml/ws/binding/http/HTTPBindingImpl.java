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
package com.sun.xml.ws.binding.http;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.sandbox.impl.TestDecoderImpl;
import com.sun.xml.ws.sandbox.impl.TestEncoderImpl;
import com.sun.xml.ws.util.localization.Localizable;
import com.sun.xml.ws.util.localization.LocalizableMessageFactory;
import com.sun.xml.ws.util.localization.Localizer;
import java.util.ArrayList;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.http.HTTPBinding;
import java.util.List;

/**
 * @author WS Development Team
 */
public class HTTPBindingImpl extends BindingImpl implements HTTPBinding {

    public HTTPBindingImpl(List<Handler> handlerChain) {
        // TODO: implement a real encoder/decoder for these
        super(handlerChain, HTTPBinding.HTTP_BINDING, null);
    }

    public Encoder createEncoder() {
        return TestEncoderImpl.INSTANCE11;
    }

    public Decoder createDecoder() {
        return TestDecoderImpl.INSTANCE11;
    }

    /**
     * This method separates the logical and protocol handlers.
     * Only logical handlers are allowed with HTTPBinding. 
     * Setting SOAPHandlers throws WebServiceException
     */
    protected void sortHandlers() {
        logicalHandlers =  new ArrayList<LogicalHandler>();
        for (Handler handler : handlers) {
            if (!(handler instanceof LogicalHandler)) {
                LocalizableMessageFactory messageFactory =
                    new LocalizableMessageFactory(
                    "com.sun.xml.ws.resources.client");
                Localizer localizer = new Localizer();
                Localizable locMessage =
                    messageFactory.getMessage("non.logical.handler.set",
                    handler.getClass().toString());
                throw new WebServiceException(localizer.localize(locMessage));
            } else {
                logicalHandlers.add((LogicalHandler) handler);
            }            
        }        
    }
    
    public SOAPVersion getSOAPVersion() {
        return null;
    }
}
