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
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.server.sei;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Headers;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.model.SEIModel;

/**
 * Puts a non-payload message parameter to {@link Message}.
 *
 * <p>
 * Instance of this class is used to handle header parameters and attachment parameters.
 * They add things to {@link Message}.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class MessageFiller {

    /**
     * The index of the method invocation parameters that this object looks for.
     */
    protected final int methodPos;

    protected MessageFiller( int methodPos) {
        this.methodPos = methodPos;
    }

    /**
     * Moves an argument of a method invocation into a {@link Message}.
     */
    abstract void fillIn(Object[] methodArgs, Object returnValue, Message msg);

    /**
     * Adds a parameter as an header.
     */
    static final class Header extends MessageFiller {
        private final SOAPVersion ver;
        private final Bridge bridge;
        private final SEIModel model;
        private final ValueGetter getter;

        protected Header(SEIModel model, int methodPos, SOAPVersion ver, Bridge bridge, ValueGetter getter) {
            super(methodPos);
            this.model = model;
            this.ver = ver;
            this.bridge = bridge;
            this.getter = getter;
        }

        void fillIn(Object[] methodArgs, Object returnValue, Message msg) {
            Object value = (methodPos == -1) ? returnValue : getter.get(methodArgs[methodPos]);
            msg.getHeaders().add(Headers.create(ver,
                bridge, model.getBridgeContext(), value));
        }
    }
}
