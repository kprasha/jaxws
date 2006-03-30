/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
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
