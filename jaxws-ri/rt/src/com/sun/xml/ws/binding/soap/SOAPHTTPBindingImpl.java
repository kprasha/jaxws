package com.sun.xml.ws.binding.soap;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.sandbox.impl.StreamSOAPDecoder;
import com.sun.xml.ws.sandbox.impl.TestEncoderImpl;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.Handler;
import java.util.List;

/**
 * SOAP/HTTP binding.
 *
 * @author Kohsuke Kawaguchi
 */
public final class SOAPHTTPBindingImpl extends SOAPBindingImpl {
    public SOAPHTTPBindingImpl(List<Handler> handlerChain, SOAPVersion soapVersion, QName serviceName) {
        super(handlerChain,soapVersion,soapVersion.httpBindingId,serviceName);
    }

    public Encoder createEncoder() {
        return TestEncoderImpl.get(soapVersion);
    }

    public Decoder createDecoder() {
        return StreamSOAPDecoder.create(soapVersion);
    }
}
