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

package com.sun.xml.ws.api.pipe.helper;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.TubeCloner;

/**
 * Convenient default implementation for filtering {@link Tube}.
 *
 * <p>
 * In this prototype, this is not that convenient, but in the real production
 * code where we have {@code preDestroy()} and {@code clone()}, this
 * is fairly handy.
 * 
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractFilterTubeImpl extends AbstractTubeImpl {
    protected final Tube next;

    protected AbstractFilterTubeImpl(Tube next) {
        this.next = next;
    }

    protected AbstractFilterTubeImpl(AbstractFilterTubeImpl that, TubeCloner cloner) {
        super(that, cloner);
        this.next = cloner.copy(that.next);
    }

    /**
     * Default no-op implementation.
     */
    public @NotNull NextAction processRequest(Packet request) {
        return doInvoke(next,request);
    }

    /**
     * Default no-op implementation.
     */
    public @NotNull NextAction processResponse(Packet response) {
        return doReturnWith(response);
    }

    /**
     * Default no-op implementation.
     */
    public @NotNull NextAction processException(Throwable t) {
        return doThrow(t);
    }

    public void preDestroy() {
        next.preDestroy();
    }
}
