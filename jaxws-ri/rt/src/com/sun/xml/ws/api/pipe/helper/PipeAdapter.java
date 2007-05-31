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
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.TubeCloner;

/**
 * {@link Tube} that invokes {@link Pipe}.
 *
 * <p>
 * This can be used to make a {@link Pipe} look like a {@link Tube}.
 *
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
public class PipeAdapter extends AbstractTubeImpl {
    private final Pipe next;

    public static Tube adapt(Pipe p) {
        if (p instanceof Tube) {
            return (Tube) p;
        } else {
            return new PipeAdapter(p);
        }
    }

    public static Pipe adapt(Tube p) {
        if (p instanceof Pipe) {
            return (Pipe) p;
        } else {
            class TubeAdapter extends AbstractPipeImpl {
                private final Tube t;

                public TubeAdapter(Tube t) {
                    this.t = t;
                }

                private TubeAdapter(TubeAdapter that, PipeCloner cloner) {
                    super(that, cloner);
                    this.t = cloner.copy(that.t);
                }

                public Packet process(Packet request) {
                    return Fiber.current().runSync(t,request);
                }

                public Pipe copy(PipeCloner cloner) {
                    return new TubeAdapter(this,cloner);
                }
            }

            return new TubeAdapter(p);
        }
    }


    private PipeAdapter(Pipe next) {
        this.next = next;
    }

    /**
     * Copy constructor
     */
    private PipeAdapter(PipeAdapter that, TubeCloner cloner) {
        super(that,cloner);
        this.next = ((PipeCloner)cloner).copy(that.next);
    }

    /**
     * Uses the current fiber and runs the whole pipe to the completion
     * (meaning everything from now on will run synchronously.)
     */
    public @NotNull NextAction processRequest(@NotNull Packet p) {
        return doReturnWith(next.process(p));
    }

    public @NotNull NextAction processResponse(@NotNull Packet p) {
        throw new IllegalStateException();
    }

    @NotNull
    public NextAction processException(@NotNull Throwable t) {
        throw new IllegalStateException();
    }

    public void preDestroy() {
        next.preDestroy();
    }

    public PipeAdapter copy(TubeCloner cloner) {
        return new PipeAdapter(this,cloner);
    }

    public String toString() {
        return super.toString()+"["+next.toString()+"]";
    }
}
