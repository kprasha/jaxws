package com.sun.xml.ws.sandbox.message.impl.jaxb;

import org.xml.sax.Attributes;

import javax.xml.bind.Marshaller;
import javax.xml.soap.SOAPConstants;

/**
 * {@link JAXBHeader} for SOAP 1.1.
 * @author Kohsuke Kawaguchi
 */
public final class JAXBHeader11 extends JAXBHeader {
    public JAXBHeader11(Marshaller marshaller, Object jaxbObject) {
        super(marshaller, jaxbObject);
    }

    public boolean isRelay() {
        return false;
    }

    @Override
    protected void checkHeaderAttribute(Attributes a) {
        for( int i=a.getLength()-1; i>=0; i-- ) {
            String uri = a.getURI(i);
            if(uri==SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE) {
                String localName = a.getLocalName(i);
                if(localName=="actor") {
                    // TODO
                } else
                    checkMustUnderstand(localName, a, i);
            }
        }
    }
}
