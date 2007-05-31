/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.protocol.soap;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.ExceptionHasMessage;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.encoding.soap.SOAP12Constants;
import com.sun.xml.ws.encoding.soap.SOAPConstants;
import com.sun.xml.ws.fault.SOAPFaultBuilder;

import javax.xml.namespace.QName;

/**
 * This is used to represent SOAP VersionMismatchFault. Use
 * this when the received soap envelope is in a different namespace
 * than what the specified Binding says.
 *
 * @author Jitendra Kotamraju
 */
public class VersionMismatchException extends ExceptionHasMessage {

    private final SOAPVersion soapVersion;

    public VersionMismatchException(SOAPVersion soapVersion, Object... args) {
        super("soap.version.mismatch.err", args);
        this.soapVersion = soapVersion;
    }

    public String getDefaultResourceBundleName() {
        return "com.sun.xml.ws.resources.soap";
    }

    public Message getFaultMessage() {
        QName faultCode = (soapVersion == SOAPVersion.SOAP_11)
            ? SOAPConstants.FAULT_CODE_VERSION_MISMATCH
            : SOAP12Constants.FAULT_CODE_VERSION_MISMATCH;
        return SOAPFaultBuilder.createSOAPFaultMessage(
                soapVersion, getLocalizedMessage(), faultCode);
    }

}
