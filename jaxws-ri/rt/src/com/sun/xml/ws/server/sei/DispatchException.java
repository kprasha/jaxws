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

package com.sun.xml.ws.server.sei;

import com.sun.xml.ws.api.message.Message;

/**
 * {@link Exception} that demands a specific fault message to be sent back.
 *
 * TODO: think about a way to generalize it, as it seems to be useful
 * in other places.
 *
 * @author Kohsuke Kawaguchi
 */
final class DispatchException extends Exception {
    final Message fault;

    public DispatchException(Message fault) {
        this.fault = fault;
    }
}
