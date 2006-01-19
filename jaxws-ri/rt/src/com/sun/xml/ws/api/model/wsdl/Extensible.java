package com.sun.xml.ws.api.model.wsdl;

import java.util.Iterator;

/**
 * @author Vivek Pandey
 */
public interface Extensible {
    public Iterator<WSDLExtension> getWSDLExtensions();
}
