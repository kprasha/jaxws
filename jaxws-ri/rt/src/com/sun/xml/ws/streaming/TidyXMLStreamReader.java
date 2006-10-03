package com.sun.xml.ws.streaming;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
import javax.xml.ws.WebServiceException;
import java.io.Closeable;
import java.io.IOException;

/**
 * Wrapper over XMLStreamReader. It will be used primarily to
 * clean up the resources such as closure on InputStream/Reader.
 *
 * @author Vivek Pandey
 */
public class TidyXMLStreamReader extends StreamReaderDelegate {
    private final Closeable closeableSource;

    public TidyXMLStreamReader(@NotNull XMLStreamReader reader, @Nullable Closeable closeableSource) {
        super(reader);
        this.closeableSource = closeableSource;
    }

    public void close() throws XMLStreamException {
        super.close();
        try {
            if(closeableSource != null)
                closeableSource.close();
        } catch (IOException e) {
            throw new WebServiceException(e);
        }
    }
}
