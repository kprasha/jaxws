package com.sun.xml.ws.wsdl.parser;

import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URL;

import com.sun.xml.ws.sandbox.server.SDDocumentSource;

/**
 * Resolves a reference to {@link XMLStreamReader}.
 *
 * This is kinda like {@link EntityResolver} but works
 * at the XML infoset level.
 *
 * @author Kohsuke Kawaguchi
 */
public interface XMLEntityResolver {
    /**
     * See {@link EntityResolver#resolveEntity(String, String)} for the contract.
     */
    Parser resolveEntity(String publicId,String systemId)
        throws SAXException, IOException, XMLStreamException;

    public static final class Parser {
        /**
         * System ID of the document being parsed.
         */
        public final URL systemId;
        /**
         * The parser instance parsing the infoset.
         */
        public final XMLStreamReader parser;

        public Parser(URL systemId, XMLStreamReader parser) {
            assert systemId!=null && parser!=null;
            this.systemId = systemId;
            this.parser = parser;
        }

        /**
         * Creates a {@link Parser} that reads from {@link SDDocumentSource}.
         */
        public Parser(SDDocumentSource doc) throws IOException, XMLStreamException {
            this.systemId = doc.getSystemId();
            this.parser = doc.read(xif);
        }

        private static final XMLInputFactory xif = XMLInputFactory.newInstance();
    }
}
