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

package com.sun.xml.ws.api.client;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;

import javax.xml.ws.handler.PortInfo;

/**
 * JAX-WS RI's extension to {@link PortInfo}.
 * 
 * @author Kohsuke Kawaguchi
 */
public interface WSPortInfo extends PortInfo {
    /**
     * Returns {@link WSService} object that owns this port.
     */
    @NotNull WSService getOwner();

    /**
     * Returns the same information as {@link #getBindingID()}
     * but in a strongly-typed fashion
     */
    @NotNull BindingID getBindingId();

    /**
     * Gets the endpoint address of this port.
     */
    @NotNull EndpointAddress getEndpointAddress();

    /**
     * Gets the {@link WSDLPort} object that represents this port,
     * if {@link WSService} is configured with WSDL. Otherwise null.
     */
    @Nullable WSDLPort getPort();
}
