package com.sun.xml.ws.developer.servlet;

import javax.xml.ws.WebServiceFeature;

/**
 * {@link WebServiceFeature} for {@link @HttpSessionScope}.
 * @author Kohsuke Kawaguchi
 */
public class HttpSessionScopeFeature extends WebServiceFeature {
    /**
     * Constant value identifying the {@link @HttpSessionScope} feature.
     */
    public static final String ID = "http://jax-ws.dev.java.net/features/servlet/httpSessionScope";

    public HttpSessionScopeFeature() {
        this.enabled = true;
    }

    public String getID() {
        return ID;
    }
}
