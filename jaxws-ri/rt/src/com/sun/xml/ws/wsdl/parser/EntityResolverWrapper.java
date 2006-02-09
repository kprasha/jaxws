package com.sun.xml.ws.wsdl.parser;

import com.sun.xml.ws.streaming.XMLStreamReaderFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;

/**
 * Wraps {@link EntityResolver} into {@link XMLEntityResolver}.
 *
 * @author Kohsuke Kawaguchi
 */
final class EntityResolverWrapper implements XMLEntityResolver {
    private final EntityResolver core;

    public EntityResolverWrapper(EntityResolver core) {
        this.core = core;
    }

    public Parser resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        InputSource source = core.resolveEntity(publicId,systemId);
        if(source==null)
            return null;    // default

        // ideally entity resolvers should be giving us the system ID for the resource
        // (or otherwise we won't be able to resolve references within this imported WSDL correctly),
        // but if none is given, the system ID before the entity resolution is better than nothing.
        if(source.getSystemId()!=null)
            systemId = source.getSystemId();

        return new Parser(new URL(systemId),
            XMLStreamReaderFactory.createFreshXMLStreamReader(source,true));
    }
}
