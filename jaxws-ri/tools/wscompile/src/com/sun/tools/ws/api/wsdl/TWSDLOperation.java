package com.sun.tools.ws.api.wsdl;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * Abstracts wsdl:portType/wsdl:operation
 *
 * @author Vivek Pandey
 */
public interface TWSDLOperation extends TWSDLExtensible{
    /**
     * Gives a Map of fault element attribute value to the fully qualified
     * exception class name.
     */
    Map<QName, String> getFaults();
}
