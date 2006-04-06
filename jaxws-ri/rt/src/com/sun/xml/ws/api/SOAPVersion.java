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

package com.sun.xml.ws.api;

import com.sun.xml.ws.encoding.soap.SOAP12Constants;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.ws.soap.SOAPBinding;


/**
 * Version of SOAP (1.1 and 1.2).
 *
 * <p>
 * This class defines various constants for SOAP 1.1 and SOAP 1.2,
 * and also defines convenience methods to simplify the processing
 * of multiple SOAP versions.
 *
 * <p>
 * This constant alows you to do:
 *
 * <pre>
 * SOAPVersion version = ...;
 * version.someOp(...);
 * </pre>
 *
 * As opposed to:
 *
 * <pre>
 * if(binding is SOAP11) {
 *   doSomeOp11(...);
 * } else {
 *   doSomeOp12(...);
 * }
 * </pre>
 *
 * @author Kohsuke Kawaguchi
 */
public enum SOAPVersion {
    SOAP_11(SOAPBinding.SOAP11HTTP_BINDING,
            com.sun.xml.ws.encoding.soap.SOAPConstants.URI_ENVELOPE,
            SOAPConstants.URI_SOAP_ACTOR_NEXT, "actor",
            javax.xml.soap.SOAPConstants.SOAP_1_1_PROTOCOL),

    SOAP_12(SOAPBinding.SOAP12HTTP_BINDING,
            SOAP12Constants.URI_ENVELOPE,
            SOAPConstants.URI_SOAP_1_2_ROLE_ULTIMATE_RECEIVER, "role",
            javax.xml.soap.SOAPConstants.SOAP_1_2_PROTOCOL);

    /**
     * Binding ID for SOAP/HTTP binding of this SOAP version.
     *
     * <p>
     * Either {@link SOAPBinding#SOAP11HTTP_BINDING} or
     *  {@link SOAPBinding#SOAP12HTTP_BINDING}
     */
    public final String httpBindingId;

    /**
     * SOAP envelope namespace URI.
     */
    public final String nsUri;

    /**
     * SAAJ {@link MessageFactory} for this SOAP version.
     */
    public final MessageFactory saajMessageFactory;

    /**
     * SAAJ {@link SOAPFactory} for this SOAP version.
     */
    public final SOAPFactory saajSoapFactory;

    /**
     * If the actor/role attribute is absent, this SOAP version assumes this value.
     */
    public final String implicitRole;

    /**
     * "role" (SOAP 1.2) or "actor" (SOAP 1.1)
     */
    public final String roleAttributeName;

    private SOAPVersion(String httpBindingId, String nsUri, String implicitRole, String roleAttributeName, String saajFactoryString) {
        this.httpBindingId = httpBindingId;
        this.nsUri = nsUri;
        this.implicitRole = implicitRole;
        this.roleAttributeName = roleAttributeName;
        try {
            saajMessageFactory = MessageFactory.newInstance(saajFactoryString);
            saajSoapFactory = SOAPFactory.newInstance(saajFactoryString);
        } catch (SOAPException e) {
            throw new Error(e);
        }
    }


    public String toString() {
        return httpBindingId;
    }

    /**
     * Returns {@link SOAPVersion} whose {@link #httpBindingId} equals to
     * the given string.
     *
     * This method does not perform input string validation.
     *
     * @param binding
     *      for historical reason, we treat null as {@link #SOAP_11},
     *      but you really shouldn't be passing null.
     * @return always non-null.
     */
    public static SOAPVersion fromHttpBinding(String binding) {
        if(binding==null)
            return SOAP_11;

        if(binding.equals(SOAP_12.httpBindingId))
            return SOAP_12;
        else
            return SOAP_11;
    }

    /**
     * Returns {@link SOAPVersion} whose {@link #nsUri} equals to
     * the given string.
     *
     * This method does not perform input string validation.
     *
     * @param nsUri
     *      must not be null.
     * @return always non-null.
     */
    public static SOAPVersion fromNsUri(String nsUri) {
        if(nsUri.equals(SOAP_12.nsUri))
            return SOAP_12;
        else
            return SOAP_11;
    }
}
