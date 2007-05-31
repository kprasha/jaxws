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

package com.sun.tools.ws.ant;

import com.sun.istack.tools.ProtectedTask;
import com.sun.tools.ws.Invoker;
import com.sun.tools.ws.resources.WscompileMessages;
import com.sun.tools.xjc.api.util.ToolsJarNotFoundException;
import com.sun.xml.bind.util.Which;
import org.apache.tools.ant.BuildException;

import javax.xml.ws.Service;
import java.io.IOException;

/**
 * Wrapper task to launch real implementations of the task in a classloader that can work
 * even in JavaSE 6.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class WrapperTask extends ProtectedTask {

    private boolean doEndorsedMagic = false;

    /**
     * Set to true to perform the endorsed directory override so that
     * Ant tasks can run on JavaSE 6. 
     */
    public void setXendorsed(boolean f) {
        this.doEndorsedMagic = f;
    }

    protected String getCoreClassName() {
        return getClass().getName()+'2';
    }

    protected ClassLoader createClassLoader() throws ClassNotFoundException, IOException {
        try {
            ClassLoader cl = getClass().getClassLoader();
            if(doEndorsedMagic) {
                return Invoker.createClassLoader(cl);
            } else {
                // check if we are indeed loading JAX-WS 2.1 API
                if(Invoker.checkIfLoading21API())
                    return cl;

                if(Service.class.getClassLoader()==null)
                    throw new BuildException(WscompileMessages.WRAPPER_TASK_NEED_ENDORSED(getTaskName()));
                else
                    throw new BuildException(WscompileMessages.WRAPPER_TASK_LOADING_20_API(Which.which(Service.class)));
            }
        } catch (ToolsJarNotFoundException e) {
            throw new ClassNotFoundException(e.getMessage(),e);
        }
    }
}
