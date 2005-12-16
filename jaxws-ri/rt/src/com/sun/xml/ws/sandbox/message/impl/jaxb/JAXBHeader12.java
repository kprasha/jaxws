package com.sun.xml.ws.sandbox.message.impl.jaxb;

import org.xml.sax.Attributes;

import javax.xml.bind.Marshaller;
import javax.xml.soap.SOAPConstants;

import com.sun.xml.ws.sandbox.message.impl.Util;

/**
 * {@link JAXBHeader} for SOAP 1.2.
 * 
 * @author Kohsuke Kawaguchi
 */
public final class JAXBHeader12 extends JAXBHeader {
    public JAXBHeader12(Marshaller marshaller, Object jaxbObject) {
        super(marshaller, jaxbObject);
    }

    public boolean isRelay() {
        parseIfNecessary();
        return isSet(FLAG_RELAY);
    }

    @Override
    protected void checkHeaderAttribute(Attributes a) {
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
}
