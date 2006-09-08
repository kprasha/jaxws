package com.sun.xml.ws.api.server;

import javax.xml.stream.XMLStreamWriter;

/**
 * TODO: javadoc
 * 
 * @author Kohsuke Kawaguchi
 */
public interface SDDocumentFilter {
    /**
     * TODO: javadoc
     */
    XMLStreamWriter filter(SDDocument doc, XMLStreamWriter w);
}
