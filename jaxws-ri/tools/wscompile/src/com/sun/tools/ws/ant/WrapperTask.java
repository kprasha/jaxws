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

    protected String getCoreClassName() {
        return getClass().getName()+'2';
    }

    protected ClassLoader createClassLoader() throws ClassNotFoundException, IOException {
        try {
            return Invoker.createClassLoader(getClass().getClassLoader());
        } catch (ToolsJarNotFoundException e) {
            throw new ClassNotFoundException(e.getMessage(),e);
        }
    }
}
