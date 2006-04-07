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

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.client.HandlerConfiguration;

import javax.xml.ws.handler.Handler;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Instances are created by the service, which then
 * sets the handler chain on the binding impl.
 *
 * <p>This class is made abstract as we dont see a situation when a BindingImpl has much meaning without binding id.
 * IOw, for a specific binding there will be a class extending BindingImpl, for example SOAPBindingImpl.
 *
 * <p>The spi Binding interface extends Binding.
 *
 * @author WS Development Team
 */
public abstract class BindingImpl implements WSBinding {

    protected List<Handler> handlers; // may be logical/soap mixed

    protected HandlerConfiguration handlerConfig;

    private final BindingID bindingId;

    protected BindingImpl(List<Handler> handlerChain, BindingID bindingId) {
        this.handlers = handlerChain;
        sortHandlers();
        this.bindingId = bindingId;
    }


    public @NotNull List<Handler> getHandlerChain() {
        if(handlers == null)
            return Collections.emptyList();
        return new ArrayList<Handler>(handlers);
    }

    public boolean hasHandlers() {
        return !handlers.isEmpty();
    }

    public HandlerConfiguration getHandlerConfig() {
        return handlerConfig;
    }


    /**
     * Sets the handlers on the binding and then
     * sorts the handlers in to logical and protocol handlers.
     */
    public void setHandlerChain(List<Handler> chain) {
        handlers = chain;
        sortHandlers();
    }

    /**
     * This is called when ever Binding.setHandlerChain() or SOAPBinding.setRoles()
     * is called.
     * This sorts out the Handlers into Logical and SOAP Handlers and
     * sets the HandlerConfiguration.
     */
    protected abstract void sortHandlers();

    public @NotNull BindingID getBindingId(){
        return bindingId;
    }

    public final SOAPVersion getSOAPVersion() {
        return bindingId.getSOAPVersion();
    }

    public final @NotNull Encoder createEncoder() {
        return bindingId.createEncoder(this);
    }

    public final @NotNull Decoder createDecoder() {
        return bindingId.createDecoder(this);
    }

    public static BindingImpl create(@NotNull BindingID bindingId) {
        if(bindingId.equals(BindingID.XML_HTTP))
            return new HTTPBindingImpl(null);
        else
            return new SOAPBindingImpl(bindingId, null, bindingId.getSOAPVersion());
    }

    public static WSBinding getDefaultBinding() {
        return new SOAPBindingImpl(BindingID.SOAP11_HTTP, null, SOAPVersion.SOAP_11);
    }
}
