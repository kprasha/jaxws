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
package com.sun.xml.ws.api.addressing;
import javax.xml.ws.soap.AddressingFeature;

/**
 * Addressing Feature representing MemberSubmission Version.
 * 
 * @author Rama Pulavarthi
 */

public class MemberSubmissionAddressingFeature extends AddressingFeature {
    /**
     * Constant value identifying the AddressingFeature
     */
    public static final String ID = "http://java.sun.com/xml/ns/jaxws/2004/08/addressing";

    /**
     * Create an AddressingFeature
     *
     * @param enabled specifies whether this feature should
     * be enabled or not.
     */
    public MemberSubmissionAddressingFeature(boolean enabled) {
        super(enabled);
    }

}
