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
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.xml.ws.api.client;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.server.Container;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;

/**
 * Gives an interception point for appserver after creating a proxy or {@link Dispatch}
 * instance. PortCreationCallback implementation could set properties(e.g. authentication properties)
 * on the created {@link BindingProvider}. {@link Container} object is used to find if
 * there is a callback object that needs to be called for these events.
 * {@link ContainerResolver#getContainer()} is used to locate {@link Container} objects
 * on the client side.
 *
 * @author Jitendra Kotamraju
 *
 * @deprecated
 * Use {@link ServiceInterceptor}. This class extends from {@link ServiceInterceptor}
 * just for the backward compatibility. 
 */
public abstract class PortCreationCallback extends ServiceInterceptor {

    /**
     * A callback to notify the event of creation of proxy object for SEI endpoint. The
     * callback could set some properties on the {@link BindingProvider}.
     *
     * @param bp created proxy instance
     * @param serviceEndpointInterface SEI of the endpoint
     */
    public void proxyCreated(WSBindingProvider bp, Class<?> serviceEndpointInterface) {
    }

    /**
     * A callback to notify that a {@link Dispatch} object is created. The callback
     * could set some properties on the {@link BindingProvider}.
     *
     * @param bp BindingProvider of dispatch object
     */
    public void dispatchCreated(WSBindingProvider bp) {
    }

// adapting new interfaces to old calls for compatibility
    public final void postCreateProxy(@NotNull WSBindingProvider bp, @NotNull Class<?> serviceEndpointInterface) {
        proxyCreated(bp,serviceEndpointInterface);
    }

    public final void postCreateDispatch(@NotNull WSBindingProvider bp) {
        dispatchCreated(bp);
    }

}
