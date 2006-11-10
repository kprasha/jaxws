package com.sun.xml.ws.developer;

import com.sun.xml.ws.api.message.Header;

import javax.xml.ws.BindingProvider;
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
     * 
     */
    void setOutboundHeaders(List<Header> headers);
    void setOutboundHeaders(Header... headers);
    void setOutboundHeaders(Object... headers);

    List<Header> getInboundHeaders();
}
