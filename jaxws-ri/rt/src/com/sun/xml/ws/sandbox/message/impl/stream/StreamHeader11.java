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
package com.sun.xml.ws.sandbox.message.impl.stream;

import com.sun.xml.stream.buffer.XMLStreamBufferMark;
import com.sun.xml.ws.sandbox.message.impl.Util;

import javax.xml.soap.SOAPConstants;
import javax.xml.stream.XMLStreamReader;

/**
 * {@link StreamHeader} for SOAP 1.1.
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class StreamHeader11 extends StreamHeader {
    
    public StreamHeader11(XMLStreamReader reader, XMLStreamBufferMark mark) {
        super(reader, mark);
    }
    
    protected final void processHeaderAttributes(XMLStreamReader reader) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final String localName = reader.getAttributeLocalName(i);
            final String namespaceURI = reader.getAttributeNamespace(i);

            if (namespaceURI == SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE) {
                if (localName == SOAP_1_1_MUST_UNDERSTAND) {
                    _isMustUnderstand = Util.parseBool(reader.getAttributeValue(i));
                } else if (localName == SOAP_1_1_ROLE) {
                    final String value = reader.getAttributeValue(i);
                    if (value != null && value.length() > 0) {
                        if (!_role.equals(SOAPConstants.URI_SOAP_ACTOR_NEXT)) {
                            _role = value;
                        } else {
                            // Normalize to SOAP 1.2 role next value
                            _role = SOAPConstants.URI_SOAP_1_2_ROLE_NEXT;
                        }
                    }
                }
            }
        }
    }
}
