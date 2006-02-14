package com.sun.xml.ws.transport.http.servlet;

import com.sun.xml.ws.transport.http.ResourceLoader;

import javax.servlet.ServletContext;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Set;

/**
 * {@link ResourceLoader} backed by {@link ServletContext}.
 *
 * @author Kohsuke Kawaguchi
 */
final class ServletResourceLoader implements ResourceLoader {
    private final ServletContext context;

    public ServletResourceLoader(ServletContext context) {
        this.context = context;
    }

    public URL getResource(String path) throws MalformedURLException {
        return context.getResource(path);
    }

    public URL getCatalogFile() throws MalformedURLException {
        return getResource("/WEB-INF/jax-ws-catalog.xml");
    }

    public Set<String> getResourcePaths(String path) {
        return context.getResourcePaths(path);
    }
}
