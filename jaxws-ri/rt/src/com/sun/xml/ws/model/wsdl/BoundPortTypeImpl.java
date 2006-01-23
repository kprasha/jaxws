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

import com.sun.xml.ws.api.model.Mode;
import com.sun.xml.ws.api.model.ParameterBinding;
import com.sun.xml.ws.api.model.soap.Style;
import com.sun.xml.ws.api.model.wsdl.BoundOperation;
import com.sun.xml.ws.api.model.wsdl.BoundPortType;
import com.sun.xml.ws.api.model.wsdl.PortType;

import javax.xml.namespace.QName;
import java.util.Hashtable;
import java.util.Map;

/**
 * Implementation of {@link BoundPortType}
 *
 * @author Vivek Pandey
 */
public final class BoundPortTypeImpl extends AbstractExtensibleImpl implements BoundPortType {
    private final QName name;
    private final QName portTypeName;
    private PortType portType;
    private String bindingId;
    private WSDLModelImpl wsdlDoc;
    private boolean finalized = false;
    private final Map<QName,BoundOperationImpl> bindingOperations = new Hashtable<QName,BoundOperationImpl>();

    public BoundPortTypeImpl(QName name, QName portTypeName) {
        this.name = name;
        this.portTypeName = portTypeName;
    }

    public QName getName() {
        return name;
    }

    public BoundOperation get(QName operationName) {
        return bindingOperations.get(operationName);
    }

    /**
     * Populates the Map that holds operation name as key and {@link BoundOperation} as the value.
     *
     * @param opName Must be non-null
     * @param ptOp   Must be non-null
     * @throws NullPointerException if either opName or ptOp is null
     */
    public void put(QName opName, BoundOperationImpl ptOp) {
        bindingOperations.put(opName,ptOp);
    }

    public QName getPortTypeName() {
        return portTypeName;
    }

    public PortType getPortType() {
        return portType;
    }

    public Iterable<BoundOperationImpl> getBindingOperations() {
        return bindingOperations.values();
    }

    public String getBindingId() {
        return bindingId;
    }

    public void setBindingId(String bindingId) {
        this.bindingId = bindingId;
    }

    public void setWsdlDocument(WSDLModelImpl wsdlDoc) {
        this.wsdlDoc = wsdlDoc;
    }

    /**
     * sets whether the {@link BoundPortType} is rpc or lit
     */
    private Style style = Style.DOCUMENT;
    public void setStyle(Style style){
        this.style = style;
    }

    public Style getStyle() {
        return style;
    }

    public boolean isRpcLit(){
        return Style.RPC.equals(style);
    }

    public boolean isDoclit(){
        return Style.DOCUMENT.equals(style);
    }


    public ParameterBinding getBinding(QName operation, String part, Mode mode) {
        BoundOperation op = get(operation);
        if (op == null) {
            //TODO throw exception
            return null;
        }
        if ((Mode.IN == mode) || (Mode.INOUT == mode))
            return op.getInputBinding(part);
        else
            return op.getOutputBinding(part);
    }

    public String getMimeType(QName operation, String part, Mode mode) {
        BoundOperation op = get(operation);
        if (Mode.IN == mode)
            return op.getMimeTypeForInputPart(part);
        else
            return op.getMimeTypeForOutputPart(part);
    }

    /**
     * This method is called to apply binings in case when a specific port is required
     */
    public void finalizeRpcLitBinding() {
        if (!finalized) {
            wsdlDoc.finalizeRpcLitBinding(this);
            finalized = true;
        }
    }

    public BoundOperation getOperation(QName tag){
        /**
         * If the style is rpc then the tag name should be
         * same as operation name so return the operation that matches the tag name.
         */
        if(style.equals(Style.RPC)){
           return bindingOperations.get(tag);
        }

        /**
         * For doclit The tag will be the operation that has the same input part descriptor value
         */
        for(BoundOperationImpl op:bindingOperations.values()){
            QName name = op.getBodyName();
            if((name != null) && name.equals(tag))
                return op;
        }

        //not found, return null
        return null;
    }

    void freeze(WSDLModelImpl owner) {
        portType = owner.getPortType(portTypeName);
        // TODO: error check for null. that's an error in WSDL that needs to be reported
        for (BoundOperationImpl op : bindingOperations.values()) {
            op.freeze(this);
        }
    }
}
