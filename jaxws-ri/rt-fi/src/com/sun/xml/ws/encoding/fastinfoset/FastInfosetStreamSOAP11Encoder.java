package com.sun.xml.ws.encoding.fastinfoset;

import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.sandbox.impl.ContentTypeImpl;

/**
 * An encoder that encodes SOAP 1.1 messages infosets to fast infoset
 * documents.
 *
 * @author Paul.Sandoz@Sun.Com
 */
final class FastInfosetStreamSOAP11Encoder extends FastInfosetStreamSOAPEncoder {
    public static final ContentTypeImpl defaultContentType = 
            new ContentTypeImpl(FastInfosetMIMETypes.SOAP_11, "");
    
    protected ContentType getContentType(String soapAction) {
        if (soapAction == null || soapAction.length() == 0) {
            return defaultContentType;
        } else {
            return new ContentTypeImpl(FastInfosetMIMETypes.SOAP_11, soapAction);
        }
    }
    
}
