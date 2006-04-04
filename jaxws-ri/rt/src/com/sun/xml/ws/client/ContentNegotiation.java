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
package com.sun.xml.ws.client;

/**
 * Content negotiation enum.
 * <p>
 * A value of {@link #none} means no content negotation at level of the 
 * client transport will be performed to negotiate the encoding of XML infoset.
 * The default encoding will always be used.
 * <p>
 * A value of {@link #pessimistic} means the client transport will assume
 * the default encoding of XML infoset for an outbound message unless informed
 * otherwise by a previously received inbound message.
 * (The client transport initially and pessimistically assumes that a service
 * does not support anything other than the default encoding of XML infoset.)
 * <p>
 * A value of {@link #optimistic} means the client transport will assume
 * a non-default encoding of XML infoset for an outbound message.
 * (The client transport optimistically assumes that a service
 * supports the non-default encoding of XML infoset.)
 *
 * @author Paul.Sandoz@Sun.Com
 */
public enum ContentNegotiation {
    none,
    pessimistic,
    optimistic;
  
    /**
     * Property name for content negotiation on a {@link RequestContext} and 
     * a {@link Packet}.
     */
    public static final String PROPERTY = "com.sun.xml.ws.client.ContentNegotiation";
    
    /**
     * Obtain the content negiation value from a system property.
     * @return the content negotiation value.
     */
    public static ContentNegotiation obtainFromSystemProperty() {
        String value = System.getProperty(PROPERTY);
        try {
            if (value == null) return none;
            
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            // Default to none for any unrecognized value
            return none;
        }
    }
}
