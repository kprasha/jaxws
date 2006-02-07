package com.sun.tools.ws.api;

import com.sun.codemodel.JMethod;
import com.sun.tools.ws.api.wsdl.TExtensible;

/**
 * Provides Java SEI Code generation Extensiblity mechanism.
 *
 * @author Vivek Pandey
 */
public abstract class JavaGeneratorExtension {
    /**
     * This method should be used to write annotations on {@link JMethod}.
     *
     * @param wsdlOperation non-null wsdl extensiblity element -  wsdl:operation.
     * @param jMethod non-null {@link JMethod}
     */
     public abstract void writeOperationAnnotations(TExtensible wsdlOperation, JMethod jMethod);
}
