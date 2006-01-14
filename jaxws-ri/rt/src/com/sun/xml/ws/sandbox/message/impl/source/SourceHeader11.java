package com.sun.xml.ws.sandbox.message.impl.source;

import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader11;
import com.sun.xml.ws.streaming.XMLStreamReaderFactory;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * @author Vivek Pandey
 */
public class SourceHeader11 extends SourceHeader{

    protected SourceHeader11(Source src) {
        super(src);
    }

    protected Header getStreamHeader() {
        StreamSource streamSource = (StreamSource)src;
        return new StreamHeader11(XMLStreamReaderFactory.createXMLStreamReader(streamSource.getInputStream(), true), null);
    }

    public boolean isMustUnderstood() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getRole() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isRelay() {
        return false;
    }
}
