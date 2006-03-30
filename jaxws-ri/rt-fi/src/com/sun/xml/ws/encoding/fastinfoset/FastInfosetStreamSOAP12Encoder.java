package com.sun.xml.ws.encoding.fastinfoset;

import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.sandbox.impl.ContentTypeImpl;

/**
 * An encoder that encodes SOAP 1.2 messages infosets to fast infoset
 * documents.
 *
 * @author Paul.Sandoz@Sun.Com
 */
final class FastInfosetStreamSOAP12Encoder extends FastInfosetStreamSOAPEncoder {
    public static final ContentTypeImpl defaultContentType = 
            new ContentTypeImpl(FastInfosetMIMETypes.SOAP_12, null);
    
    protected ContentType getContentType(String soapAction) {
        if (soapAction == null) {
            return defaultContentType;
        } else {
            return new ContentTypeImpl(FastInfosetMIMETypes.SOAP_12 + ";action=\""+soapAction+"\"", null);
        }
    }
    
}
