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
package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.api.pipe.helper.AbstractTubeImpl;

import java.util.HashMap;
import java.util.Map;

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
    // Pipe to pipe, or tube to tube
    private final Map<Object,Object> master2copy = new HashMap<Object,Object>();

    /**
     * {@link Pipe} version of {@link #clone(Tube)}
     */
    public static Pipe clone(Pipe p) {
        return new PipeCloner().copy(p);
    }

    /**
     * Invoked by a client of a tube to clone the whole pipeline.
     *
     * <p>
     * {@link Tube}s implementing the {@link Tube#copy(PipeCloner)} method
     * shall use {@link #copy(Tube)} method.
     *
     * @param p
     *      The entry point of a pipeline to be copied. must not be null.
     * @return
     *      The cloned pipeline. Always non-null.
     */
    public static Tube clone(Tube p) {
        return new PipeCloner().copy(p);
    }

    // no need to be constructed publicly. always use the static clone method.
    private PipeCloner() {}

    /**
     * {@link Pipe} version of {@link #copy(Tube)}
     */
    public <T extends Pipe> T copy(T p) {
        Pipe r = (Pipe)master2copy.get(p);
        if(r==null) {
            r = p.copy(this);
            // the pipe must puts its copy to the map by itself
            assert master2copy.get(p)==r : "the pipe must call the add(...) method to register itself before start copying other pipes, but "+p+" hasn't done so";
        }
        return (T)r;
    }

    /**
     * Invoked by a {@link Tube#copy(PipeCloner)} implementation
     * to copy a reference to another pipe.
     *
     * <p>
     * This method is for {@link Tube} implementations, not for users.
     *
     * <p>
     * If the given tube is already copied for this cloning episode,
     * this method simply returns that reference. Otherwise it copies
     * a tube, make a note, and returns a copied tube. This additional
     * step ensures that a graph is cloned isomorphically correctly.
     *
     * <p>
     * (Think about what happens when a graph is A->B, A->C, B->D, and C->D
     * if you don't have this step.)
     *
     * @param t
     *      The tube to be copied.
     * @return
     *      The cloned tube. Always non-null.
     */
    public <T extends Tube> T copy(T t) {
        Tube r = (Tube)master2copy.get(t);
        if(r==null) {
            r = t.copy(this);
            // the pipe must puts its copy to the map by itself
            assert master2copy.get(t)==r : "the tube must call the add(...) method to register itself before start copying other pipes, but "+t +" hasn't done so";
        }
        return (T)r;
    }

    /**
     * The {@link Pipe} version of {@link #add(Tube, Tube)}.
     */
    public void add(Pipe original, Pipe copy) {
        assert !master2copy.containsKey(original);
        assert original!=null && copy!=null;
        master2copy.put(original,copy);
    }

    /**
     * This method must be called from within the copy constructor
     * to notify that the copy was created.
     *
     * <p>
     * When your pipe has references to other pipes,
     * it's particularly important to call this method
     * before you start copying the pipes you refer to,
     * or else there's a chance of inifinite loop.
     */
    public void add(Tube original, Tube copy) {
        assert !master2copy.containsKey(original);
        assert original!=null && copy!=null;
        master2copy.put(original,copy);
    }

    /**
     * Disambiguation version.
     */
    public void add(AbstractTubeImpl original, AbstractTubeImpl copy) {
        add((Tube)original,copy);
    }
}
