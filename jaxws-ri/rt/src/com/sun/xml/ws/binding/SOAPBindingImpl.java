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
import com.sun.xml.ws.encoding.soap.streaming.SOAP12NamespaceConstants;
import com.sun.xml.ws.encoding.soap.streaming.SOAPNamespaceConstants;
import com.sun.xml.ws.handler.HandlerException;
import com.sun.xml.ws.spi.runtime.SystemHandlerDelegateFactory;
import com.sun.xml.ws.util.localization.Localizable;
import com.sun.xml.ws.util.localization.LocalizableMessageFactory;
import com.sun.xml.ws.util.localization.Localizer;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPFactory;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.soap.SOAPBinding;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



/**
 * @author WS Development Team
 */
public final class SOAPBindingImpl extends BindingImpl implements SOAPBinding {

    public static final String X_SOAP12HTTP_BINDING =
        "http://java.sun.com/xml/ns/jaxws/2003/05/soap/bindings/HTTP/";

    protected static String ROLE_NONE;

    protected Set<String> requiredRoles;
    protected Set<String> roles;
    protected boolean enableMtom;
    private Set<QName> handlerUnderstoodHeaders;
    protected final SOAPVersion soapVersion;


    /**
     * Use {@link BindingImpl#create(BindingID, QName)} to create this.
     */
    SOAPBindingImpl(
        BindingID bindingId, List<Handler> handlerChain, SOAPVersion soapVersion, QName serviceName) {

        super(handlerChain, bindingId, serviceName);
        this.soapVersion = soapVersion;
        setup();
        setupSystemHandlerDelegate(serviceName);

        this.enableMtom = bindingId.isMTOMEnabled();
    }

    // if the binding id is unknown, no roles are added
    private void setup() {
        requiredRoles = new HashSet<String>();
        switch(soapVersion) {
        case SOAP_11:
            requiredRoles.add(SOAPNamespaceConstants.ACTOR_NEXT);
            break;
        case SOAP_12:
            requiredRoles.add(SOAP12NamespaceConstants.ROLE_NEXT);
            requiredRoles.add(SOAP12NamespaceConstants.ROLE_ULTIMATE_RECEIVER);
        }
        ROLE_NONE = SOAP12NamespaceConstants.ROLE_NONE;
        roles = new HashSet<String>();
        addRequiredRoles();        
    }
    
    /**
     * This method separates the logical and protocol handlers. 
     * Also parses Headers understood by SOAPHandlers.
     */
    protected void sortHandlers() {
        handlerUnderstoodHeaders = new HashSet<QName>();
        logicalHandlers =  new ArrayList<LogicalHandler>();
        soapHandlers =  new ArrayList<SOAPHandler>();
        if(handlers == null)
            return;
        for (Handler handler : handlers) {
            if (LogicalHandler.class.isAssignableFrom(handler.getClass())) {
                logicalHandlers.add((LogicalHandler) handler);
            } else if (SOAPHandler.class.isAssignableFrom(handler.getClass())) {
                soapHandlers.add((SOAPHandler) handler);
                Set<QName> headers = ((SOAPHandler<?>) handler).getHeaders();
                if (headers != null) {
                    handlerUnderstoodHeaders.addAll(headers);
                }
            } else if (Handler.class.isAssignableFrom(handler.getClass())) {
                throw new HandlerException(
                    "cannot.extend.handler.directly",
                    handler.getClass().toString());
            } else {
                throw new HandlerException("handler.not.valid.type",
                    handler.getClass().toString());
            }
        }
        //handlers.clear();
        //handlers.addAll(logicalHandlers);
        //handlers.addAll(soapHandlers);
    }
    
    /**
     * Returns the headers understood by the handlers. This set
     * is created when the handler chain caller is instantiated and
     * the handlers are sorted. The set is comprised of headers
     * returned from SOAPHandler.getHeaders() method calls.
     *
     * @return The set of all headers that the handlers declare
     * that they understand.
     */
    public Set<QName> getHandlerUnderstoodHeaders() {
        return handlerUnderstoodHeaders;
    }

    protected void addRequiredRoles() {
        roles.addAll(requiredRoles);
    }

    public Set<String> getRoles() {
        return roles;
    }

    /*
     * Adds the next and other roles in case this has
     * been called by a user without them.
     */
    public void setRoles(Set<String> roles) {
        if (roles == null) {
            roles = new HashSet<String>();
        }
        if (roles.contains(ROLE_NONE)) {
            LocalizableMessageFactory messageFactory =
                new LocalizableMessageFactory("com.sun.xml.ws.resources.client");
            Localizer localizer = new Localizer();
            Localizable locMessage =
                messageFactory.getMessage("invalid.soap.role.none");
            throw new WebServiceException(localizer.localize(locMessage));
        }
        this.roles = roles;
        addRequiredRoles();        
    }


    /**
     * Used typically by the runtime to enable/disable Mtom optimization
     */
    public boolean isMTOMEnabled() {
        return enableMtom;
    }

    /**
     * Client application can override if the MTOM optimization should be enabled
     */
    public void setMTOMEnabled(boolean b) {
        this.enableMtom = b;
    }

    public SOAPFactory getSOAPFactory() {
        return soapVersion.saajSoapFactory;
    }

    public MessageFactory getMessageFactory() {
        return soapVersion.saajMessageFactory;
    }

    protected void setupSystemHandlerDelegate(QName serviceName) {
        SystemHandlerDelegateFactory shdFactory =
            SystemHandlerDelegateFactory.getFactory();
        if (shdFactory != null) {
            setSystemHandlerDelegate(shdFactory.getDelegate(serviceName));
        }
    }
}
