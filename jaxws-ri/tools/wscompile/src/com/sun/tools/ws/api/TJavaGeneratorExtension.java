package com.sun.tools.ws.api;

import com.sun.codemodel.JMethod;
import com.sun.tools.ws.api.wsdl.TWSDLOperation;
import com.sun.tools.ws.processor.generator.JavaGeneratorExtensionFacade;

/**
 * Provides Java SEI Code generation Extensiblity mechanism.
 *
 * @see JavaGeneratorExtensionFacade
 * @author Vivek Pandey
 */
public abstract class TJavaGeneratorExtension {
    /**
     * This method should be used to write annotations on {@link JMethod}.
     *
     * @param wsdlOperation non-null wsdl extensiblity element -  wsdl:portType/wsdl:operation.
     * @param jMethod non-null {@link JMethod}
     */
     public abstract void writeMethodAnnotations(TWSDLOperation wsdlOperation, JMethod jMethod);
}
