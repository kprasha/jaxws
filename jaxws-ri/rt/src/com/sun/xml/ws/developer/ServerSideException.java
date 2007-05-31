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

package com.sun.xml.ws.developer;

/**
 * Represents the exception that has occurred on the server side.
 *
 * <p>
 * When an exception occurs on the server, JAX-WS RI sends the stack
 * trace of that exception to the client. On the client side,
 * instances of this class are used to represent such stack trace.
 *
 * @author Kohsuke Kawaguchi
 * @since 2.1
 */
public class ServerSideException extends Exception {
    private final String className;

    public ServerSideException(String className, String message) {
        super(message);
        this.className = className;
    }

    public String toString() {
        String s = className;
        String message = getLocalizedMessage();
        return (message != null) ? (s + ": " + message) : s;
    }
}
