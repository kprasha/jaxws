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

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.TubeCloner;

/**
 * Base class for {@link Tube} implementation.
 *
 * <p>
 * This can be also used as a {@link Pipe}, and thus effectively
 * making every {@link Tube} usable as a {@link Pipe}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractTubeImpl implements Tube, Pipe {

    /**
     * Default constructor.
     */
    protected AbstractTubeImpl() {
    }

    /**
     * Copy constructor.
     */
    protected AbstractTubeImpl(AbstractTubeImpl that, TubeCloner cloner) {
        cloner.add(that,this);
    }

    protected final NextAction doInvoke(Tube next, Packet packet) {
        NextAction na = new NextAction();
        na.invoke(next,packet);
        return na;
    }

    protected final NextAction doInvokeAndForget(Tube next, Packet packet) {
        NextAction na = new NextAction();
        na.invokeAndForget(next,packet);
        return na;
    }

    protected final NextAction doReturnWith(Packet response) {
        NextAction na = new NextAction();
        na.returnWith(response);
        return na;
    }

    protected final NextAction doSuspend() {
        NextAction na = new NextAction();
        na.suspend();
        return na;
    }

    protected final NextAction doThrow(Throwable t) {
        NextAction na = new NextAction();
        na.throwException(t);
        return na;
    }

    /**
     * "Dual stack" compatibility mechanism.
     * Allows {@link Tube} to be invoked from a {@link Pipe}.
     */
    public Packet process(Packet p) {
        return Fiber.current().runSync(this,p);
    }

    /**
     * Needs to be implemented by the derived class, but we can't make it abstract
     * without upsetting javac.
     */
    public final AbstractTubeImpl copy(PipeCloner cloner) {
        return copy((TubeCloner)cloner);
    }

    public abstract AbstractTubeImpl copy(TubeCloner cloner);
}
