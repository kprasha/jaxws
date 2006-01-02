package com.sun.xml.ws.sandbox.message.impl;

import com.sun.xml.ws.sandbox.message.Message;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

/**
 * Partial {@link Message} implementation.
 *
 * <p>
 * This class implements some of the {@link Message} methods in a default,
 * possibly slow way. Derived classes should examine carefully
 * which method can be implemented faster.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractMessageImpl extends Message {
    public Source readEnvelopeAsSource() {
        return new SAXSource(new XMLReaderImpl(this), XMLReaderImpl.THE_SOURCE);
    }

    public <T> T readPayloadAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        return (T)unmarshaller.unmarshal(readPayloadAsSource());
    }
    
    // TODO: expand
}
