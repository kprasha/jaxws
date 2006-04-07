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
package com.sun.xml.ws.client;

import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.namespace.QName;
import java.util.Set;
import java.util.List;
import java.util.HashSet;

/**
 * @author Rama Pulavarthi
 */

/**
 * This class holds the handler information on the BindingProvider.
 * HandlerConfiguration is immutable, and a new object is created
 * when the BindingImpl is created or User calls Binding.setHandlerChain() or
 * SOAPBinding.setRoles()
 * During inovcation in Stub.process(), snapshot of the handler configuration is set in
 * Packet.handlerConfig
 * The information in the HandlerConfiguration is used by MUPipe and HandlerPipe
 * implementations.
 */
public class HandlerConfiguration {
    private final Set<String> roles;
    private final List<LogicalHandler> logicalHandlers;
    private final List<SOAPHandler> soapHandlers;
    private Set<QName> knownHeaders;

    /**
     * @param roles                    This contains the roles assumed by the Binding implementation.
     * @param portUnderstoodHeaders    This contains the headers that are bound to the current WSDL Port
     * @param logicalHandlers
     * @param soapHandlers
     * @param handlerUnderstoodHeaders The set is comprised of headers returned from SOAPHandler.getHeaders()
     *                                 method calls.
     */
    public HandlerConfiguration(Set<String> roles, Set<QName> portUnderstoodHeaders,
                                List<LogicalHandler> logicalHandlers, List<SOAPHandler> soapHandlers,
                                Set<QName> handlerUnderstoodHeaders) {
        this.roles = roles;
        this.logicalHandlers = logicalHandlers;
        this.soapHandlers = soapHandlers;
        this.knownHeaders = new HashSet<QName>();
        knownHeaders.addAll(portUnderstoodHeaders);
        knownHeaders.addAll(handlerUnderstoodHeaders);
    }

    public Set<String> getRoles() {
        return roles;
    }

    public List<LogicalHandler> getLogicalHandlers() {
        return logicalHandlers;
    }

    public List<SOAPHandler> getSoapHandlers() {
        return soapHandlers;
    }

    public Set<QName> getKnownHeaders() {
        return knownHeaders;
    }

}
