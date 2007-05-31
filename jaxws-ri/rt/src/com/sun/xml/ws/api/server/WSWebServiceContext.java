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

package com.sun.xml.ws.api.server;

import com.sun.istack.Nullable;
import com.sun.xml.ws.api.message.Packet;

import javax.xml.ws.WebServiceContext;

/**
 * {@link WebServiceContext} that exposes JAX-WS RI specific additions.
 *
 * <p>
 * {@link WebServiceContext} instances that JAX-WS injects always
 * implement this interface.
 *
 * <p>
 * The JAX-WS RI may add methods on this interface, so do not implement
 * this interface in your code, or risk {@link LinkageError}.
 *
 * @author Kohsuke Kawaguchi
 */
public interface WSWebServiceContext extends WebServiceContext {
    /**
     * Obtains the request packet that is being processed.
     * @return Packet for the request
     */
    @Nullable Packet getRequestPacket();
}
