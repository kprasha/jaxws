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

package com.sun.xml.ws.transport.local;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.pipe.ClientTubeAssemblerContext;
import com.sun.xml.ws.api.pipe.TransportTubeFactory;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.server.WSEndpoint;

import javax.xml.ws.WebServiceException;
import java.net.URI;

/**
 * {@link TransportTubeFactory} that recognizes
 * "in-vm://<i>inVmServerId</i>[?<i>portLocalName</i>]".
 */
public final class InVmTransportFactory extends TransportTubeFactory {
    public Tube doCreate(@NotNull ClientTubeAssemblerContext context) {
        URI adrs = context.getAddress().getURI();
        if(!adrs.getScheme().equals("in-vm") && !adrs.getScheme().equals("in-vm-async"))
            return null;

        String serverId = adrs.getAuthority();
        InVmServer server = InVmServer.get(serverId);
        if(server==null)
            throw new WebServiceException("No such server is running: "+adrs);
        WSEndpoint endpoint;
        if(server.getEndpoints().size()==1)
            endpoint = server.getEndpoints().get(0);
        else
            endpoint = server.getByPortName(adrs.getQuery());
        if(endpoint==null)
            throw new WebServiceException("No such port exists: "+adrs);

        // maybe I should be passing in my custom adapter
        if(adrs.getScheme().equals("in-vm"))
            return new LocalTransportTube(adrs,endpoint,context.getCodec());
        else
            return new LocalAsyncTransportTube(adrs,endpoint,context.getCodec());
    }
}
