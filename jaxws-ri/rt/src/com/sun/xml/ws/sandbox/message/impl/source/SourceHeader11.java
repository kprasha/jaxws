package com.sun.xml.ws.sandbox.message.impl.source;

import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader11;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader;
import com.sun.xml.ws.streaming.XMLStreamReaderFactory;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.soap.SOAPConstants;

import org.xml.sax.Attributes;

/**
 * @author Vivek Pandey
 */
public class SourceHeader11 extends SourceHeader{

    protected SourceHeader11(Source src) {
        super(src);
        if(sourceUtils.isStreamSource()){
            sh = getStreamHeader();
        }
    }

    /**
     * This method is called only incase that the {@link Source} is instanceof
     * {@link javax.xml.transform.sax.SAXSource} or {@link javax.xml.transform.dom.DOMSource}
     * @param a
     */
    @Override
    protected void checkHeaderAttribute(Attributes a) {
        if(sourceUtils.isStreamSource())
            return;
        for( int i=a.getLength()-1; i>=0; i-- ) {
            String uri = a.getURI(i);
            if(uri== SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE) {
                String localName = a.getLocalName(i);
                if(localName=="actor") {
                    role = a.getValue(i);
                } else
                    checkMustUnderstand(localName, a, i);
            }
        }
    }

    private StreamHeader getStreamHeader() {
        StreamSource streamSource = (StreamSource)src;
        return new StreamHeader11(XMLStreamReaderFactory.createXMLStreamReader(streamSource.getInputStream(), true), null);
    }

    public boolean isRelay() {
        return false;
    }
}
