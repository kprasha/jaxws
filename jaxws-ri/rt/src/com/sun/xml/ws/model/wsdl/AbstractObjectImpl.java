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

package com.sun.xml.ws.model.wsdl;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.model.wsdl.WSDLObject;
import org.xml.sax.Locator;
import org.xml.sax.helpers.LocatorImpl;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamReader;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractObjectImpl implements WSDLObject {
    // source location information
    private final int lineNumber;
    private final String systemId;


    /*package*/ AbstractObjectImpl(XMLStreamReader xsr) {
        Location loc = xsr.getLocation();
        this.lineNumber = loc.getLineNumber();
        this.systemId = loc.getSystemId();
    }

    /*package*/ AbstractObjectImpl(String systemId, int lineNumber) {
        this.systemId = systemId;
        this.lineNumber = lineNumber;
    }

    public final @NotNull Locator getLocation() {
        LocatorImpl loc = new LocatorImpl();
        loc.setSystemId(systemId);
        loc.setLineNumber(lineNumber);
        return loc;
    }
}
