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

import com.sun.xml.ws.api.model.wsdl.WSDLPortType;
import com.sun.xml.ws.api.model.wsdl.WSDLOperation;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Hashtable;

/**
 * Provides implementation of {@link WSDLPortType}
 *
 * @author Vivek Pandey
 */
public final class WSDLPortTypeImpl  extends AbstractExtensibleImpl implements WSDLPortType {
    private QName name;
    private final Map<String, WSDLOperationImpl> portTypeOperations;

    public WSDLPortTypeImpl(QName name) {
        super();
        this.name = name;
        portTypeOperations = new Hashtable<String, WSDLOperationImpl>();
    }

    public QName getName() {
        return name;
    }

    public WSDLOperationImpl get(String operationName) {
        return portTypeOperations.get(operationName);
    }

    Iterable<WSDLOperationImpl> getOperations() {
        return portTypeOperations.values();
    }

    /**
     * Populates the Map that holds operation name as key and {@link WSDLOperation} as the value.
     * @param opName Must be non-null
     * @param ptOp  Must be non-null
     * @throws NullPointerException if either opName or ptOp is null
     */
    public void put(String opName, WSDLOperationImpl ptOp){
        portTypeOperations.put(opName, ptOp);
    }

    void freeze(WSDLModelImpl root) {
        for(WSDLOperationImpl op : portTypeOperations.values()){
            op.freez(root);            
        }
    }
}
