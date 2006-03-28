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
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.spi.runtime.SystemHandlerDelegate;
import com.sun.istack.NotNull;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.soap.SOAPHandler;
import java.util.ArrayList;
import java.util.List;

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

    private SystemHandlerDelegate systemHandlerDelegate;
    protected List<Handler> handlers; // may be logical/soap mixed
    protected List<LogicalHandler> logicalHandlers;
    protected List<SOAPHandler> soapHandlers;
    
    
    private final BindingID bindingId;
    protected final QName serviceName;

    protected BindingImpl(List<Handler> handlerChain, BindingID bindingId, QName serviceName) {
        this.handlers = handlerChain;
        sortHandlers();
        this.bindingId = bindingId;
        this.serviceName = serviceName;
    }


    /**
     * Return a copy of the list. If there is a handler chain caller,
     * this is the proper list. Otherwise, return a copy of 'handlers'.
     *
     * @return The list of handlers.
     * The list may have a different order depending on
     * whether or not the handlers have been called yet, since the
     * logical and protocol handlers will be sorted before calling them.
     */
    public List<Handler> getHandlerChain() {
        if(handlers == null)
            return null;
        return new ArrayList<Handler>(handlers);
    }

    public boolean hasHandlers() {
        return !handlers.isEmpty();
    }

    /**
     * Sets the handlers on the binding and then
     * sorts the handlers in to logical and protocol handlers.
     */
    public void setHandlerChain(List<Handler> chain) {
        handlers = chain;
        sortHandlers();
    }
    
    protected abstract void sortHandlers();
    
    public List<SOAPHandler> getSOAPHandlerChain(){
        return soapHandlers;
    }
    
    public List<LogicalHandler> getLogicalHandlerChain(){
        return logicalHandlers;
    }
    
    public @NotNull BindingID getBindingId(){
        return bindingId;
    }

    public final SOAPVersion getSOAPVersion() {
        return bindingId.getSOAPVersion();
    }

    public final @NotNull Encoder createEncoder() {
        return bindingId.createEncoder();
    }

    public final @NotNull Decoder createDecoder() {
        return bindingId.createDecoder();
    }

    public SystemHandlerDelegate getSystemHandlerDelegate() {
        return systemHandlerDelegate;
    }

    public void setSystemHandlerDelegate(SystemHandlerDelegate delegate) {
        systemHandlerDelegate = delegate;
    }

    public static BindingImpl create(@NotNull BindingID bindingId, QName serviceName) {
        if(bindingId.equals(BindingID.XML_HTTP))
            return new HTTPBindingImpl(null);
        else
            return new SOAPBindingImpl(bindingId, null, bindingId.getSOAPVersion(), serviceName);
    }

    public static WSBinding getDefaultBinding() {
        return getDefaultBinding(null);
    }

    public static WSBinding getDefaultBinding(QName serviceName) {
        return new SOAPBindingImpl(BindingID.SOAP11_HTTP, null, SOAPVersion.SOAP_11, serviceName);
    }
}
