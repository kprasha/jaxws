package com.sun.xml.ws.api.model.wsdl;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.WSFeatureList;

import javax.xml.ws.WebServiceFeature;

/**
 * {@link WSDLObject} that can have features associated.
 * 
 * @author Kohsuke Kawaguchi
 */
public interface WSDLFeaturedObject extends WSDLObject {
    /**
     * Returns the {@link WebServiceFeature} that matches the given <code>id</code>.
     *
     * @param id unique id of the feature
     * @return WebServiceFeature matching the id
     * @deprecated
     *      Use {@link #getFeature( Class)}
     */
    @Nullable
    WebServiceFeature getFeature(String id);

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
