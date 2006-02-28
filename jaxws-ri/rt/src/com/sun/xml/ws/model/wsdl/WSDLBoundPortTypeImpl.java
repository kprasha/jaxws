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

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.model.ParameterBinding;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundPortType;
import com.sun.xml.ws.util.QNameMap;
import com.sun.istack.NotNull;

import javax.jws.WebParam.Mode;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import javax.xml.namespace.QName;
import java.util.Map;

/**
 * Implementation of {@link WSDLBoundPortType}
 *
 * @author Vivek Pandey
 */
public final class WSDLBoundPortTypeImpl extends AbstractExtensibleImpl implements WSDLBoundPortType {
    private final QName name;
    private final QName portTypeName;
    private WSDLPortTypeImpl portType;
    private String bindingId;
    private final @NotNull WSDLModelImpl owner;
    private boolean finalized = false;
    private final QNameMap<WSDLBoundOperationImpl> bindingOperations = new QNameMap<WSDLBoundOperationImpl>();

    /**
     * Operations keyed by the payload tag name.
     */
    private QNameMap<WSDLBoundOperationImpl> payloadMap;
    /**
     * {@link #payloadMap} doesn't allow null key, so we store the value for it here.
     */
    private WSDLBoundOperationImpl emptyPayloadOperation;



    public WSDLBoundPortTypeImpl(@NotNull WSDLModelImpl owner, QName name, QName portTypeName) {
        this.owner = owner;
        this.name = name;
        this.portTypeName = portTypeName;
        owner.addBinding(this);
    }

    public QName getName() {
        return name;
    }

    public @NotNull WSDLModelImpl getOwner() {
        return owner;
    }

    public WSDLBoundOperationImpl get(QName operationName) {
        return bindingOperations.get(operationName);
    }

    /**
     * Populates the Map that holds operation name as key and {@link WSDLBoundOperation} as the value.
     *
     * @param opName Must be non-null
     * @param ptOp   Must be non-null
     * @throws NullPointerException if either opName or ptOp is null
     */
    public void put(QName opName, WSDLBoundOperationImpl ptOp) {
        bindingOperations.put(opName,ptOp);
    }

    public QName getPortTypeName() {
        return portTypeName;
    }

    public WSDLPortTypeImpl getPortType() {
        return portType;
    }

    public Iterable<WSDLBoundOperationImpl> getBindingOperations() {
        return bindingOperations.values();
    }

    public String getBindingId() {
        return bindingId;
    }

    public void setBindingId(String bindingId) {
        this.bindingId = bindingId;
    }

    /**
     * sets whether the {@link WSDLBoundPortType} is rpc or lit
     */
    private Style style = Style.DOCUMENT;
    public void setStyle(Style style){
        this.style = style;
    }

    public SOAPBinding.Style getStyle() {
        return style;
    }

    public boolean isRpcLit(){
        return Style.RPC==style;
    }

    public boolean isDoclit(){
        return Style.DOCUMENT==style;
    }


    public ParameterBinding getBinding(QName operation, String part, Mode mode) {
        WSDLBoundOperation op = get(operation);
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
        WSDLBoundOperation op = get(operation);
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
            owner.finalizeRpcLitBinding(this);
            finalized = true;
        }
    }

    public WSDLBoundOperation getOperation(String namespaceUri, String localName) {
        if(namespaceUri==null && localName == null)
            return emptyPayloadOperation;
        else
            return payloadMap.get(namespaceUri,localName);
    }

    public SOAPVersion getSOAPVersion(){
        return SOAPVersion.fromHttpBinding(bindingId);
    }

    void freeze() {
        portType = owner.getPortType(portTypeName);
        // TODO: error check for portType==null. that's an error in WSDL that needs to be reported
        portType.freeze(owner);

        for (WSDLBoundOperationImpl op : bindingOperations.values()) {
            op.freeze(this);
        }

        freezePayloadMap();
    }

    private void freezePayloadMap() {
        if(style== Style.RPC) {
            // If the style is rpc then the tag name should be
            // same as operation name so return the operation that matches the tag name.
            payloadMap = bindingOperations;
        } else {
            payloadMap = new QNameMap<WSDLBoundOperationImpl>();
            // For doclit The tag will be the operation that has the same input part descriptor value
            for(WSDLBoundOperationImpl op : bindingOperations.values()){
                QName name = op.getPayloadName();
                if(name == null){
                    //empty payload
                    emptyPayloadOperation = op;
                    continue;
                }

                payloadMap.put(name, op);
            }
        }
    }
}
