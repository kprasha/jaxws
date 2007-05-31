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

package com.sun.xml.ws.api.server;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;

/**
 * Provides a way to filter {@link SDDocument} infoset while writing it. These
 * filter objects can be added to {@link ServiceDefinition} using
 * {@link ServiceDefinition#addFilter(SDDocumentFilter)}
 * 
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
public interface SDDocumentFilter {
    /**
     * Returns a wrapped XMLStreamWriter on top of passed-in XMLStreamWriter.
     * It works like any filtering API for e.g. {@link java.io.FilterOutputStream}.
     * The method returns a XMLStreamWriter that calls the same methods on original
     * XMLStreamWriter with some modified events. The end result is some infoset
     * is filtered before it reaches the original writer and the infoset writer
     * doesn't have to change any code to incorporate this filter.
     *
     * @param doc gives context for the filter. This should only be used to query
     *  read-only information. Calling doc.writeTo() may result in infinite loop.
     * @param w Original XMLStreamWriter
     * @return Filtering {@link XMLStreamWriter}
     */
    XMLStreamWriter filter(SDDocument doc, XMLStreamWriter w) throws XMLStreamException, IOException;
}
