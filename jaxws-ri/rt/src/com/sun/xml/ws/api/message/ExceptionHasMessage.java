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

package com.sun.xml.ws.api.message;

import com.sun.xml.ws.util.exception.JAXWSExceptionBase;
import com.sun.xml.ws.protocol.soap.VersionMismatchException;

/**
 * This class represents an Exception that needs to be marshalled
 * with a specific protocol wire format. For example, the SOAP's
 * VersionMismatchFault needs to be written with a correct fault code.
 * In that case, decoder could throw {@link VersionMismatchException},
 * and the correspoinding fault {@link Message} from {@link ExceptionHasMessage::getFaultMessage}
 * is sent on the wire.
 *
 * @author Jitendra Kotamraju
 */
public abstract class ExceptionHasMessage extends JAXWSExceptionBase {

    public ExceptionHasMessage(String key, Object... args) {
        super(key, args);
    }

    /**
     * Returns the exception into a fault Message
     *
     * @return Message for this exception
     */
    public abstract Message getFaultMessage();
}
