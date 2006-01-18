package com.sun.xml.ws.sandbox.message.impl.source;

import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader12;
import com.sun.xml.ws.sandbox.message.impl.Util;
import com.sun.xml.ws.streaming.XMLStreamReaderFactory;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.soap.SOAPConstants;

import org.xml.sax.Attributes;

/**
 * @author Vivek Pandey
 */
public class SourceHeader12 extends SourceHeader{

    public SourceHeader12(Source src) {
        super(src);
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
            if(uri== SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE) {
                String localName = a.getLocalName(i);
                if(localName=="role") {
                    role = a.getValue(i);
                } else
                if(localName=="relay" && Util.parseBool(a.getValue(i))) {
                    set(FLAG_RELAY);
                } else
                checkMustUnderstand(localName, a, i);
            }
        }
    }

    private Header getStreamHeader() {
       StreamSource streamSource = (StreamSource)src;
       return new StreamHeader12(XMLStreamReaderFactory.createXMLStreamReader(streamSource.getInputStream(), true), null);
   }

    public boolean isRelay() {
        parseIfNecessary();
        return isSet(FLAG_RELAY);
    }
}
