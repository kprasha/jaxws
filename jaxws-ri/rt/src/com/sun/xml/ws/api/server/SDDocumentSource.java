package com.sun.xml.ws.api.server;

import com.sun.xml.stream.buffer.XMLStreamBuffer;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URL;

/**
 * SPI that provides the source of {@link SDDocument}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class SDDocumentSource {
    /**
     * Returns the {@link XMLStreamReader} that reads the document.
     *
     * <p>
     * This method maybe invoked multiple times concurrently.
     *
     * @param xif
     *      The implementation may choose to use this object when it wants to
     *      create a new parser (or it can just ignore this parameter completely.)
     *
     * @throws XMLStreamException
     *      if something goes wrong while creating a parser.
     * @throws IOException
     *      if something goes wrong trying to read the document.
     */
    public abstract XMLStreamReader read(XMLInputFactory xif) throws IOException, XMLStreamException;

    /**
     * System ID of this document.
     */
    public abstract URL getSystemId();

    /**
     * Creates {@link SDDocumentSource} from an URL.
     */
    public static SDDocumentSource create(final URL url) {
        return new SDDocumentSource() {
            private final URL systemId = url;

            public XMLStreamReader read(XMLInputFactory xif) throws IOException, XMLStreamException {
                return xif.createXMLStreamReader(systemId.toExternalForm(),url.openStream());
            }

            public URL getSystemId() {
                return systemId;
            }
        };
    }

    /**
     * Creates a {@link SDDocumentSource} from {@link XMLStreamBuffer}.
     */
    public static SDDocumentSource create(final URL systemId, final XMLStreamBuffer xsb) {
        return new SDDocumentSource() {
            public XMLStreamReader read(XMLInputFactory xif) throws XMLStreamException {
                return xsb.readAsXMLStreamReader();
            }

            public URL getSystemId() {
                return systemId;
            }
        };
    }
}
