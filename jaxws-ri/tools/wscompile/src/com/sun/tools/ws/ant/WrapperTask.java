package com.sun.tools.ws.ant;

import com.sun.istack.tools.ProtectedTask;
import com.sun.tools.ws.Invoker;
import com.sun.tools.xjc.api.util.ToolsJarNotFoundException;

import java.io.IOException;

/**
 * Wrapper task to launch real implementations of the task in a classloader that can work
 * even in JavaSE 6.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class WrapperTask extends ProtectedTask {

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
                cl = Invoker.createClassLoader(cl);
            }
            return cl;
        } catch (ToolsJarNotFoundException e) {
            throw new ClassNotFoundException(e.getMessage(),e);
        }
    }
}
