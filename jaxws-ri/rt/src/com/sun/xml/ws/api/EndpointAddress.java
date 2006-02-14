package com.sun.xml.ws.api;

import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

/**
 * Represents the endpoint address.
 *
 * <p>
 * Conceptually this can be really thought of as an {@link URL},
 * but it hides some of the details that improve the performance.
 *
 * <h3>How it improves the performance</h3>
 * <ol>
 *  <li>
 *  Endpoint address is eventually turned into an {@link URL},
 *  and given that generally this value is read more often than being set,
 *  it makes sense to eagerly turn it into an {@link URL},
 *  thereby avoiding a repeated conversion.
 *
 *  <li>
 *  JDK spends a lot of time choosing a list of {@link Proxy}
 *  to connect to an {@link URL}. Since the default proxy selector
 *  implementation always return the same proxy for the same URL,
 *  we can determine the proxy by ourselves to let JDK skip its
 *  proxy-discovery step.
 *
 *  (That said, user-defined proxy selector can do a lot of interesting things
 *  --- like doing a round-robin, or pick one from a proxy farm randomly,
 *  and so it's dangerous to stick to one proxy. For this case,
 *  we still let JDK decide the proxy. This shouldn't be that much of an
 *  disappointment, since most people only mess with system properties,
 *  and never with {@link ProxySelector}. Also, avoiding optimization
 *  with non-standard proxy selector allows people to effectively disable
 *  this optimization, which may come in handy for a trouble-shooting.)
 * </ol>
 *
 * @author Kohsuke Kawaguchi
 */
public final class EndpointAddress {
    private final URL url;
    private final String stringForm;
    private final Proxy proxy;

    public EndpointAddress(URL url) {
        this.url = url;
        this.stringForm = url.toString();
        proxy = chooseProxy();
    }

    /**
     *
     * @see #create(String)
     */
    public EndpointAddress(String url) throws MalformedURLException {
        this.url = new URL(url);
        this.stringForm = url;
        proxy = chooseProxy();
    }

    /**
     * Creates a new {@link EndpointAddress} with a reasonably
     * generic error handling.
     */
    public static EndpointAddress create(String url) {
        try {
            return new EndpointAddress(url);
        } catch(MalformedURLException e) {
            throw new WebServiceException("Illegal endpoint address: "+url,e);
        }
    }

    private Proxy chooseProxy() {
        ProxySelector sel =
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<ProxySelector>() {
                    public ProxySelector run() {
                        return ProxySelector.getDefault();
                    }
                });

        if(sel==null)
            return Proxy.NO_PROXY;


        if(!sel.getClass().getName().equals("sun.net.spi.DefaultProxySelector"))
            // user-defined proxy. may return a different proxy for each invocation
            return null;

        try {
            Iterator<Proxy> it = sel.select(new URI(stringForm)).iterator();
            if(it.hasNext())
                return it.next();
        } catch (URISyntaxException e) {
            // this shouldn't happen, but since it did,
            // take the safer route and let the JDK decide proxy on its own
            return null;
        }

        return Proxy.NO_PROXY;
    }

    public URL getURL() {
        return url;
    }

    public URLConnection openConnection() throws IOException {
        return url.openConnection(proxy);
    }

    public String toString() {
        return stringForm;
    }
}
