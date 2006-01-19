package com.sun.xml.ws.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.Extensible;
import com.sun.xml.ws.api.model.wsdl.WSDLExtension;

import java.util.Iterator;
import java.util.Set;

/**
 * All the WSDL 1.1 elements that are extensible should subclass from this abstract implementation of
 * {@link Extensible} interface.
 *
 * @author Vivek Pandey
 */
abstract public class AbstractExtensibleImpl implements Extensible {
    protected Set<WSDLExtension> extensions;

    public Iterator<WSDLExtension> getWSDLExtensions() {
        return extensions.iterator();
    }

    public void addExtension(WSDLExtension ex) {
        extensions.add(ex);
    }
}
