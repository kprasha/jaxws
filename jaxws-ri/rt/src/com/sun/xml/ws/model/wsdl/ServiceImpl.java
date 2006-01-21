/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.ws.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.Port;
import com.sun.xml.ws.api.model.wsdl.Service;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of {@link Service}
 *
 * @author Vivek Pandey
 */
public final class ServiceImpl extends AbstractExtensibleImpl implements Service {
    private final QName name;
    private final Map<QName, Port> ports;

    public ServiceImpl(QName name) {
        super();
        this.name = name;
        ports = new LinkedHashMap<QName, Port>();
    }

    public QName getName() {
        return name;
    }

    public Port get(QName portName) {
        return ports.get(portName);
    }

    public Iterable<Port> getPorts(){
        return ports.values();
    }

    /**
     * Populates the Map that holds port name as key and {@link Port} as the value.
     *
     * @param portName Must be non-null
     * @param port     Must be non-null
     * @throws NullPointerException if either opName or ptOp is null
     */
    public void put(QName portName, Port port) {
        if (portName == null || port == null)
            throw new NullPointerException();
        ports.put(portName, port);
    }
}