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
package com.sun.xml.ws.client;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

/**
 * Information about a port dynamically added through {@link Service#addPort(QName, String, String)}.
 *
 * @author JAXWS Development Team
 */
final class PortInfoBase {

    private final String targetEndpoint;
    private final QName portName;
    private final String bindingId;

    public PortInfoBase(String targetEndpoint, QName name, String bindingId) {
        this.targetEndpoint = targetEndpoint;
        this.portName = name;
        this.bindingId = bindingId;
    }

    public QName getName() {
        return portName;
    }

    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public String getBindingId() {
        return bindingId;
    }
}
