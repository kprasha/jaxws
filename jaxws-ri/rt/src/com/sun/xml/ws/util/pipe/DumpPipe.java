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

package com.sun.xml.ws.util.pipe;

import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.TubeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractTubeImpl;
import com.sun.xml.ws.api.pipe.helper.PipeAdapter;

import java.io.PrintStream;

/**
 * {@link Pipe} that dumps messages that pass through.
 *
 * @deprecated
 *      until we complete migration to tube
 * @author Kohsuke Kawaguchi
 */
public class DumpPipe extends DumpTube {

    /**
     * @deprecated
     *      until we complete migration to tube.
     */
    public DumpPipe(String name, PrintStream out, Pipe next) {
        this(name,out,PipeAdapter.adapt(next));
    }

    public DumpPipe(String name, PrintStream out, Tube next) {
        super(name, out, next);
    }

    /**
     * @param out
     *      The output to send dumps to.
     * @param next
     *      The next {@link Pipe} in the pipeline.
     *
     * @deprecated
     *      use {@link #DumpPipe(String, PrintStream, Pipe)}
     */
    public DumpPipe(PrintStream out, Pipe next) {
        this("DumpPipe",out,next);
    }

    /**
     * Copy constructor.
     */
    private DumpPipe(DumpPipe that, TubeCloner cloner) {
        super(that,cloner);
    }

    public AbstractTubeImpl copy(TubeCloner cloner) {
        return new DumpPipe(this,cloner);
    }
}
