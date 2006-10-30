/*
 The contents of this file are subject to the terms
 of the Common Development and Distribution License
 (the "License").  You may not use this file except
 in compliance with the License.
 
 You can obtain a copy of the license at
 https://jwsdp.dev.java.net/CDDLv1.0.html
 See the License for the specific language governing
 permissions and limitations under the License.
 
 When distributing Covered Code, include this CDDL
 HEADER in each file and include the License file at
 https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 add the following below this CDDL HEADER, with the
 fields enclosed by brackets "[]" replaced with your
 own identifying information: Portions Copyright [yyyy]
 [name of copyright owner]
*/
/*
 $Id: EndpointMethodDispatcher.java,v 1.1.2.4 2006-10-30 19:22:41 arungupta Exp $

 Copyright (c) 2006 Sun Microsystems, Inc.
 All rights reserved.
*/

package com.sun.xml.ws.server.sei;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.Message;

/**
 * This interface needs to be implemented if a new dispatching
 * mechanism needs to be plugged in. The dispatcher is plugged in the
 * constructor of {@link EndpointMethodDispatcherGetter}.
 *
 * @author Arun Gupta
 * @see EndpointMethodDispatcherGetter
 */
public interface EndpointMethodDispatcher {
    /**
     * Returns the {@link EndpointMethodHandler} for the <code>request</code>
     * {@link Packet}.
     *
     * @param request request packet
     * @return EndpointMethodHandler for the request packet
     */
    public EndpointMethodHandler getEndpointMethodHandler(Packet request);

    /**
     * Returns a String representation of the key from {@link Packet}
     * used for extracting the {@link EndpointMethodHandler}.
     *
     * @return key used for dispatching
     */
    public String getDispatchKey();

    /**
     * Returns a name for {@link this} dispatcher.
     *
     * @return
     */
    public String getName();

    /**
     * Returns the fault {@link Message} if null handler is returned
     * when {@link #getEndpointMethodHandler(Packet)} is invoked. 
     *
     * @return
     */
    public Message getFaultMessage();
}
