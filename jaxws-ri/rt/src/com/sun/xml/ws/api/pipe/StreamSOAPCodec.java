/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.api.pipe;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.message.Message;

import javax.xml.stream.XMLStreamReader;

/**
 * Reads events from {@link XMLStreamReader} and constructs a
 * {@link Message} for SOAP envelope. {@link Codecs} allows a
 * way to construct a whole codec that can handle MTOM, MIME
 * encoded packages using this codec.
 *
 *
 * @see Codecs
 * @author Jitendra Kotamraju
 */
public interface StreamSOAPCodec extends Codec {
    /**
     * Reads events from {@link XMLStreamReader} and constructs a
     * {@link Message} for SOAP envelope.
     *
     * @param reader that represents SOAP envelope infoset
     * @return a {@link Message} for SOAP envelope
     */
    public @NotNull Message decode(@NotNull XMLStreamReader reader);
}
