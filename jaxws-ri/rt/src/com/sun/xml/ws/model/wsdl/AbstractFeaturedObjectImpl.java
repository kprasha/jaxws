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

package com.sun.xml.ws.model.wsdl;import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.model.wsdl.WSDLFeaturedObject;
import com.sun.xml.ws.binding.WebServiceFeatureList;

import javax.xml.stream.XMLStreamReader;
import javax.xml.ws.WebServiceFeature;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractFeaturedObjectImpl extends AbstractExtensibleImpl implements WSDLFeaturedObject {
    protected WebServiceFeatureList features;

    protected AbstractFeaturedObjectImpl(XMLStreamReader xsr) {
        super(xsr);
    }
    protected AbstractFeaturedObjectImpl(String systemId, int lineNumber) {
        super(systemId, lineNumber);
    }

    public final void addFeature(WebServiceFeature feature) {
        if (features == null)
            features = new WebServiceFeatureList();

        features.add(feature);
    }

    public @NotNull WebServiceFeatureList getFeatures() {
        if(features == null)
            return new WebServiceFeatureList();
        return features;
    }

    public final WebServiceFeature getFeature(String id) {
        if (features != null) {
            for (WebServiceFeature f : features) {
                if (f.getID().equals(id))
                    return f;
            }
        }

        return null;
    }

    @Nullable
    public <F extends WebServiceFeature> F getFeature(@NotNull Class<F> featureType) {
        if(features==null)
            return null;
        else
            return features.get(featureType);
    }
}
