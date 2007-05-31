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

package com.sun.xml.ws.util.exception;

import com.sun.istack.NotNull;
import com.sun.xml.ws.resources.UtilMessages;
import org.xml.sax.Locator;
import org.xml.sax.helpers.LocatorImpl;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamReader;
import javax.xml.ws.WebServiceException;
import java.util.Arrays;
import java.util.List;

/**
 * {@link WebServiceException} with source location informaiton.
 *
 * <p>
 * This exception should be used wherever the location information is available,
 * so that the location information is carried forward to users (to assist
 * error diagnostics.)
 *
 * @author Kohsuke Kawaguchi
 */
public class LocatableWebServiceException extends WebServiceException {
    /**
     * Locations related to error.
     */
    private final Locator[] location;

    public LocatableWebServiceException(String message, Locator... location) {
        this(message,null,location);
    }

    public LocatableWebServiceException(String message, Throwable cause, Locator... location) {
        super(appendLocationInfo(message,location), cause);
        this.location = location;
    }

    public LocatableWebServiceException(Throwable cause, Locator... location) {
        this(cause.toString(),cause,location);
    }

    public LocatableWebServiceException(String message, XMLStreamReader locationSource) {
        this(message,toLocation(locationSource));
    }

    public LocatableWebServiceException(String message, Throwable cause, XMLStreamReader locationSource) {
        this(message,cause,toLocation(locationSource));
    }

    public LocatableWebServiceException(Throwable cause, XMLStreamReader locationSource) {
        this(cause,toLocation(locationSource));
    }

    /**
     * Locations related to this exception.
     *
     * @return
     *      Can be empty but never null.
     */
    public @NotNull List<Locator> getLocation() {
        return Arrays.asList(location);
    }

    private static String appendLocationInfo(String message, Locator[] location) {
        StringBuilder buf = new StringBuilder(message);
        for( Locator loc : location )
            buf.append('\n').append(UtilMessages.UTIL_LOCATION( loc.getLineNumber(), loc.getSystemId() ));
        return buf.toString();
    }

    private static Locator toLocation(XMLStreamReader xsr) {
        LocatorImpl loc = new LocatorImpl();
        Location in = xsr.getLocation();
        loc.setSystemId(in.getSystemId());
        loc.setPublicId(in.getPublicId());
        loc.setLineNumber(in.getLineNumber());
        loc.setColumnNumber(in.getColumnNumber());
        return loc;
    }
}
