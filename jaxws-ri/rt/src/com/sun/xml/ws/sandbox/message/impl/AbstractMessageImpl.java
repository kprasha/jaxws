/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.ws.sandbox.message.impl;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

/**
 * Partial {@link Message} implementation.
 *
 * <p>
 * This class implements some of the {@link Message} methods.
 * The idea is that those implementations may be non-optimal but
 * it may save effort in implementing {@link Message} and reduce
 * the code size.
 *
 * <p>
 * {@link Message} classes that are used more commonly should
 * examine carefully which method can be implemented faster,
 * and override them accordingly.
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

    public <T> T readPayloadAsJAXB(Bridge<T> bridge, BridgeContext context) throws JAXBException {
        return bridge.unmarshal(context,readPayloadAsSource());
    }

    // TODO: expand
}
