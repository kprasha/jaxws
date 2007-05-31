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

package com.sun.xml.ws.developer;

import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.Headers;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.Service.Mode;
import java.util.List;

/**
 * {@link BindingProvider} with JAX-WS RI's extension methods.
 *
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 * @since 2.1EA3
 */
public interface WSBindingProvider extends BindingProvider {
    /**
     * Sets the out-bound headers to be added to messages sent from
     * this {@link BindingProvider}.
     *
     * <p>
     * Calling this method would discard any out-bound headers
     * that were previously set.
     *
     * <p>
     * A new {@link Header} object can be created by using
     * one of the methods on {@link Headers}.
     *
     * @param headers
     *      The headers to be added to the future request messages.
     *      To clear the outbound headers, pass in either null
     *      or empty list.
     * @throws IllegalArgumentException
     *      if the list contains null item.
     */
    void setOutboundHeaders(List<Header> headers);

    /**
     * Sets the out-bound headers to be added to messages sent from
     * this {@link BindingProvider}.
     *
     * <p>
     * Works like {@link #setOutboundHeaders(List)} except
     * that it accepts a var arg array.
     *
     * @param headers
     *      Can be null or empty.
     */
    void setOutboundHeaders(Header... headers);

    /**
     * Sets the out-bound headers to be added to messages sent from
     * this {@link BindingProvider}.
     *
     * <p>
     * Each object must be a JAXB-bound object that is understood
     * by the {@link JAXBContext} object known by this {@link WSBindingProvider}
     * (that is, if this is a {@link Dispatch} with JAXB, then
     * {@link JAXBContext} given to {@link Service#createDispatch(QName,JAXBContext,Mode)}
     * and if this is a typed proxy, then {@link JAXBContext}
     * implicitly created by the JAX-WS RI.)
     *
     * @param headers
     *      Can be null or empty.
     * @throws UnsupportedOperationException
     *      If this {@lini WSBindingProvider} is a {@link Dispatch}
     *      that does not use JAXB.
     */
    void setOutboundHeaders(Object... headers);

    List<Header> getInboundHeaders();
}
