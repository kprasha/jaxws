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

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.binding.http.HTTPBindingImpl;
import com.sun.xml.ws.binding.soap.SOAPBindingImpl;
import com.sun.xml.ws.binding.soap.SOAPHTTPBindingImpl;
import com.sun.xml.ws.handler.HandlerException;
import com.sun.xml.ws.model.RuntimeModeler;
import com.sun.xml.ws.spi.runtime.SystemHandlerDelegate;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.SOAPBinding;
import java.util.ArrayList;
import java.util.Collections;
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
    
    
    private final String bindingId;
    protected final QName serviceName;

    // called by DispatchImpl
    protected BindingImpl(List<Handler> handlerChain, String bindingId, QName serviceName) {
        if(handlerChain==null)
            handlerChain = Collections.emptyList();
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
    
    public String getBindingId(){
        return bindingId;
    }

    public String getActualBindingId() {
        return bindingId;
    }

    public SystemHandlerDelegate getSystemHandlerDelegate() {
        return systemHandlerDelegate;
    }

    public void setSystemHandlerDelegate(SystemHandlerDelegate delegate) {
        systemHandlerDelegate = delegate;
    }

    /**
     * @deprecated
     *      This ugly it-does-all method needs refactoring!
     */
    public static WSBinding getBinding(String bindingId, Class implementorClass, QName serviceName, boolean tokensOK) {

        if (bindingId == null) {
            // Gets bindingId from @BindingType annotation
            bindingId = RuntimeModeler.getBindingId(implementorClass);
            if (bindingId == null) {            // Default one
                bindingId = SOAPBinding.SOAP11HTTP_BINDING;
            }
        }
        if (tokensOK) {
            if (bindingId.equals("##SOAP11_HTTP")) {
                bindingId = SOAPBinding.SOAP11HTTP_BINDING;
            } else if (bindingId.equals("##SOAP12_HTTP")) {
                bindingId = SOAPBinding.SOAP12HTTP_BINDING;
            } else if (bindingId.equals("##XML_HTTP")) {
                bindingId = HTTPBinding.HTTP_BINDING;
            }
        }
        if (bindingId.equals(SOAPBinding.SOAP11HTTP_BINDING)
            || bindingId.equals(SOAPBinding.SOAP12HTTP_BINDING)
            || bindingId.equals(SOAPBindingImpl.X_SOAP12HTTP_BINDING)) {
            return new SOAPHTTPBindingImpl(null, SOAPVersion.fromHttpBinding(bindingId), serviceName);
        } else if (bindingId.equals(HTTPBinding.HTTP_BINDING)) {
            return new HTTPBindingImpl(null);
        } else {
            throw new IllegalArgumentException("Wrong bindingId "+bindingId);
        }
    }

    public static WSBinding getDefaultBinding() {
        return getDefaultBinding(null);
    }

    public static WSBinding getDefaultBinding(QName serviceName) {
        return new SOAPHTTPBindingImpl(null,SOAPVersion.SOAP_11,serviceName);
    }
}
