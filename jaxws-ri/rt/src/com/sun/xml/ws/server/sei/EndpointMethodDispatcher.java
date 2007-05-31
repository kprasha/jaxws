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
/*
 * $Id: EndpointMethodDispatcher.java,v 1.1.2.5.2.1 2007-05-31 23:40:58 ofung Exp $
 */

package com.sun.xml.ws.server.sei;

import com.sun.xml.ws.api.message.Packet;

/**
 * This interface needs to be implemented if a new dispatching
 * mechanism needs to be plugged in. The dispatcher is plugged in the
 * constructor of {@link EndpointMethodDispatcherGetter}.
 *
 * @author Arun Gupta
 * @see EndpointMethodDispatcherGetter
 */
interface EndpointMethodDispatcher {
    /**
     * Returns the {@link EndpointMethodHandler} for the <code>request</code>
     * {@link Packet}.
     *
     * @param request request packet
     * @return
     *      non-null {@link EndpointMethodHandler} that will route the request packet.
     *      null to indicate that the request packet be processed by the next available
     *      {@link EndpointMethodDispatcher}.
     * @throws DispatchException
     *      If the request is invalid, and processing shall be aborted with a specific fault.
     */
    EndpointMethodHandler getEndpointMethodHandler(Packet request) throws DispatchException;
}
