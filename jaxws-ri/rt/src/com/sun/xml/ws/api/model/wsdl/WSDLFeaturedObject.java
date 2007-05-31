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

package com.sun.xml.ws.api.model.wsdl;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.WSFeatureList;
import com.sun.xml.ws.api.wsdl.parser.WSDLParserExtension;

import javax.xml.ws.Dispatch;
import javax.xml.ws.WebServiceFeature;

/**
 * {@link WSDLObject} that can have features associated with it.
 *
 * <p>
 * {@link WSDLParserExtension}s can add features to this object,
 * which will then be incorporated when {@link Dispatch}s and
 * proxies are created for the port.
 *
 * @author Kohsuke Kawaguchi
 */
public interface WSDLFeaturedObject extends WSDLObject {

    @Nullable
    <F extends WebServiceFeature> F getFeature(@NotNull Class<F> featureType);

    /**
     * Gets the feature list associated with this object.
     */
    @NotNull WSFeatureList getFeatures();

    /**
     * Enables a {@link WebServiceFeature} based upon policy assertions on this port.
     * This method would be called during WSDL parsing by WS-Policy code.
     */
    void addFeature(@NotNull WebServiceFeature feature);
}
