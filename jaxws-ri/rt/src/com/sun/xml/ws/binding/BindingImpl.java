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

import javax.xml.namespace.QName;
import javax.xml.ws.handler.Handler;
import java.util.List;
import java.util.Set;
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

    private HandlerConfiguration handlerConfig;
    protected final Set<QName> portKnownHeaders = Collections.<QName>emptySet();
    private final BindingID bindingId;

    protected BindingImpl(BindingID bindingId) {
        this.bindingId = bindingId;
    }


    public @NotNull List<Handler> getHandlerChain() {
        return handlerConfig.getHandlerChain();
    }

    public HandlerConfiguration getHandlerConfig() {
        return handlerConfig;
    }


    /**
     * Sets the handlers on the binding and then
     * sorts the handlers in to logical and protocol handlers.
     * Creates a new HandlerConfiguration object and sets it on the BindingImpl.
     */
    public void setHandlerChain(List<Handler> chain) {
        setHandlerConfig(createHandlerConfig(chain,handlerConfig.getRoles(), portKnownHeaders));
    }

    /**
     * This is called when ever Binding.setHandlerChain() or SOAPBinding.setRoles()
     * is called.
     * This sorts out the Handlers into Logical and SOAP Handlers and
     * sets the HandlerConfiguration.
     */
    protected void setHandlerConfig(HandlerConfiguration handlerConfig) {
        this.handlerConfig = handlerConfig;
    }

    protected abstract HandlerConfiguration createHandlerConfig(List<Handler> handlerChain, Set<String> roles,
                                                                Set<QName> portKnownHeaders);
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
            return new HTTPBindingImpl(Collections.<Handler>emptyList());
        else
            return new SOAPBindingImpl(bindingId, Collections.<Handler>emptyList(), bindingId.getSOAPVersion());
    }

    public static WSBinding getDefaultBinding() {
        return new SOAPBindingImpl(BindingID.SOAP11_HTTP, Collections.<Handler>emptyList(), SOAPVersion.SOAP_11);
    }
}
