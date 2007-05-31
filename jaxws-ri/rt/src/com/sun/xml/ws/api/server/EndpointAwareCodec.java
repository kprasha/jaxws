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

package com.sun.xml.ws.api.server;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.pipe.Codec;

/**
 * Implemented by {@link Codec}s that want to have access to
 * {@link WSEndpoint} object.
 *
 * @author Kohsuke Kawaguchi
 * @since 2.1.1
 */
public interface EndpointAwareCodec extends Codec {
    /**
     * Called by the {@linK WSEndpoint} implementation
     * when the codec is associated with an endpoint.
     */
    void setEndpoint(@NotNull WSEndpoint endpoint);
}
