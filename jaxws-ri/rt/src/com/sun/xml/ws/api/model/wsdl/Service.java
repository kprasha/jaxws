package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.Extensible;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Iterator;

/**
 * Abstracts wsdl:service.
 *
 * @author Vivek Pandey
 */
public interface Service extends Extensible {
    /**
     * Gets the name of the wsdl:service@name attribute value as local name and wsdl:definitions@targetNamespace
     * as the namespace uri.
     */
    QName getName();

    /**
     * Gets the {@link Port} for a given port name
     *
     * @param portName non-null operationName
     * @return null if a {@link Port} is not found
     */
    Port get(QName portName);

    /**
     * Gets the first {@link Port} if any, or otherwise null.
     */
    Port getFirstPort();

    Iterable<? extends Port> getPorts();
}
