/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.ws.sandbox.pipe;

import java.util.Map;
import java.util.HashMap;

/**
 * Clones the whole pipeline.
 *
 * <p>
 * Since {@link Pipe}s may form an arbitrary directed graph, someone needs
 * to keep track of isomorphism for a clone to happen correctly. This class
 * serves that role.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PipeCloner {
    private final Map<Pipe,Pipe> master2copy = new HashMap<Pipe,Pipe>();

    /**
     * Invoked by a client of a pipe to clone the whole pipeline.
     *
     * <p>
     * {@link Pipe}s implementing the {@link Pipe#copy(PipeCloner)} method
     * shall use {@link #copy(Pipe)} method.
     *
     * @param p
     *      The entry point of a pipeline to be copied. must not be null.
     * @return
     *      The cloned pipeline. Always non-null.
     */
    public static Pipe clone(Pipe p) {
        return new PipeCloner().copy(p);
    }

    // no need to be constructed publicly. always use the static clone method.
    private PipeCloner() {}

    /**
     * Invoked by a {@link Pipe#copy(PipeCloner)} implementation
     * to copy a reference to another pipe.
     *
     * <p>
     * This method is for {@link Pipe} implementations, not for users.
     *
     * <p>
     * If the given pipe is already copied for this cloning episode,
     * this method simply returns that reference. Otherwise it copies
     * a pipe, make a note, and returns a copied pipe. This additional
     * step ensures that a graph is cloned isomorphically correctly.
     *
     * <p>
     * (Think about what happens when a graph is A->B, A->C, B->D, and C->D
     * if you don't have this step.)
     *
     * @param p
     *      The pipe to be copied.
     * @return
     *      The cloned pipe. Always non-null.
     */
    public <T extends Pipe> T copy(T p) {
        Pipe r = master2copy.get(p);
        if(r==null) {
            r = p.copy(this);
            master2copy.put(p,r);
        }
        return (T)r;
    }
}
