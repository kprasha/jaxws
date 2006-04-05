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

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;

import javax.xml.namespace.QName;

/**
 * Information about a port.
 *
 * This object is owned by {@link WSServiceDelegate} to keep track of a port,
 * since a port maybe added dynamically.
 *
 * @author JAXWS Development Team
 */
class PortInfo {
    private final @NotNull WSServiceDelegate owner;

    public final @NotNull QName portName;
    public final @NotNull EndpointAddress targetEndpoint;
    public final @NotNull BindingID bindingId;

    /**
     * If a port is known statically to a WSDL, {@link PortInfo} may
     * have the corresponding WSDL model. This would occur when the
     * service was created with the WSDL location and the port is defined
     * in the WSDL.
     */
    public final WSDLPort portModel;

    public PortInfo(WSServiceDelegate owner, EndpointAddress targetEndpoint, QName name, BindingID bindingId, WSDLPort port) {
        this.owner = owner;
        this.targetEndpoint = targetEndpoint;
        this.portName = name;
        this.bindingId = bindingId;
        this.portModel = port;
    }

    public BindingImpl createBinding() {
        return owner.createBinding(portName,bindingId);
    }

    /**
     * Gets the {@link WSDLPort} model for this port, if any
     *
     * <p>
     * A port may be dynamically added by an user without having the corresponding WSDL
     * definition, in which case this method returns null.
     *
     * @return
     *      can be null.
     */
    public @Nullable WSDLPort getWSDLModel() {
        return portModel;
    }
}
