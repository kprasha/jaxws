package com.sun.xml.ws.sandbox.message;

import java.util.List;
import javax.xml.ws.handler.MessageContext;
import java.util.Map;
import javax.xml.ws.handler.MessageContext.Scope;

/**
 * Properties associated with a {@link Message}.
 *
 * <p>
 * This implements {@link MessageContext}, as we want this object to
 * be returned where user applications expect {@link MessageContext}.
 *
 * <p>
 * {@link LogicalMessageContext} and {@link SOAPMessageContext} will
 * be implemented as a delegate to this object, since those interfaces
 * may replace the {@link Message} object.
 *
 * <p>
 * If there are properties known the JAX-WS statically, they should be
 * present on this class as fields. We can generate a {@link Map} view
 * of those when requested.
 */
public abstract /*for now*/ class MessageProperties implements MessageContext {
    /* value of MessageContext.HTTP_REQUEST_HEADERS property */
    private Map<String, List<String>> httpRequestHeaders;
    
    /* value of MessageContext.HTTP_RESPONSE_HEADERS property */
    private Map<String, List<String>> httpResponseHeaders;
    
    public void setHttpRequestHeaders(Map<String, List<String>> headers) {
        this.httpRequestHeaders = headers;
    }
    
    public Map<String, List<String>> getHttpRequestHeaders() {
        return httpRequestHeaders;
    }

}
