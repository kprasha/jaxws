package com.sun.xml.ws.api;import com.sun.istack.NotNull;
import com.sun.istack.Nullable;

import javax.xml.ws.WebServiceFeature;

/**
 * Read-only list of {@link WebServiceFeature}s.
 *
 * @author Kohsuke Kawaguchi
 */
public interface WSFeatureList extends Iterable<WebServiceFeature> {
    /**
     * Find out if a particular {@link WebServiceFeature} is enabled or not
     * @param featureId
     * @return true if feature is enabled, false otherwise
     * @deprecated
     *      Use {@link #isFeatureEnabled(Class)}
     */
    boolean isFeatureEnabled(String featureId);

    /**
     * Checks if a particular {@link WebServiceFeature} is enabled.
     *
     * @return
     *      true if enabled.
     */
    boolean isFeatureEnabled(@NotNull Class<? extends WebServiceFeature> feature);


    /**
     * @param featureId
     * @return WebServiceFeature if the feature is enabled.
     *         null if is is not enabled, or not present.
     * @deprecated
     *      Use {@link #getFeature(Class)}
     */
    @Nullable WebServiceFeature getFeature(String featureId);

    /**
     * Gets a {@link WebServiceFeature} of the specific type.
     *
     * @param featureType
     *      The type of the feature to retrieve.
     * @return
     *      If the feature is present and enabled, return a non-null instance.
     *      Otherwise null.
     */
    @Nullable <F extends WebServiceFeature> F getFeature(@NotNull Class<F> featureType);
}
