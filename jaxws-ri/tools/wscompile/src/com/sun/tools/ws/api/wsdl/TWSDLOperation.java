package com.sun.tools.ws.api.wsdl;

import com.sun.codemodel.JClass;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * Abstracts wsdl:portType/wsdl:operation
 *
 * @author Vivek Pandey
 */
public interface TWSDLOperation extends TWSDLExtensible{
    /**
     * Gives a Map of fault name attribute value to the {@link JClass}
     */
    Map<String, JClass> getFaults();
}
