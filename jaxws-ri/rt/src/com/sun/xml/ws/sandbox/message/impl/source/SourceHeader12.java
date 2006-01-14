package com.sun.xml.ws.sandbox.message.impl.source;

import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader12;
import com.sun.xml.ws.streaming.XMLStreamReaderFactory;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * @author Vivek Pandey
 */
public class SourceHeader12 extends SourceHeader{

    public SourceHeader12(Source src) {
        super(src);
    }

     protected Header getStreamHeader() {
        StreamSource streamSource = (StreamSource)src;
        return new StreamHeader12(XMLStreamReaderFactory.createXMLStreamReader(streamSource.getInputStream(), true), null);
    }

    public boolean isMustUnderstood() {
        return false;
    }

    public String getRole() {
        return null;
    }

    public boolean isRelay() {
        return false;
    }
}
