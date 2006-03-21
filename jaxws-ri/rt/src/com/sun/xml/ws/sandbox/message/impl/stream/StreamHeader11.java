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

import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferException;
import com.sun.xml.ws.sandbox.message.impl.Util;
import com.sun.istack.FinalArrayList;

import javax.xml.soap.SOAPConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import java.util.List;
import java.util.ArrayList;

/**
 * {@link StreamHeader} for SOAP 1.1.
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class StreamHeader11 extends StreamHeader {

    public StreamHeader11(XMLStreamReader reader, XMLStreamBuffer mark) {
        super(reader, mark);
    }

    public StreamHeader11(XMLStreamReader reader) throws XMLStreamBufferException, XMLStreamException {
        super(reader);
    }

    protected final FinalArrayList<Attribute> processHeaderAttributes(XMLStreamReader reader) {
        FinalArrayList<Attribute> atts = null;

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final String localName = reader.getAttributeLocalName(i);
            final String namespaceURI = reader.getAttributeNamespace(i);
            final String value = reader.getAttributeValue(i);

            if (namespaceURI == SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE) {
                if (localName == SOAP_1_1_MUST_UNDERSTAND) {
                    _isMustUnderstand = Util.parseBool(value);
                } else if (localName == SOAP_1_1_ROLE) {
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

            if(atts==null) {
                atts = new FinalArrayList<Attribute>();
            }
            atts.add(new Attribute(namespaceURI,localName,value));
        }

        return atts;
    }
}
