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

import com.sun.xml.ws.api.model.ParameterBinding;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundPortType;
import com.sun.xml.ws.api.model.wsdl.WSDLPortType;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.util.QNameMap;

import javax.xml.namespace.QName;
import javax.jws.WebParam.Mode;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import java.util.Hashtable;
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
    private WSDLModelImpl wsdlDoc;
    private boolean finalized = false;
    private final Map<QName, WSDLBoundOperationImpl> bindingOperations = new Hashtable<QName, WSDLBoundOperationImpl>();

    public WSDLBoundPortTypeImpl(QName name, QName portTypeName) {
        this.name = name;
        this.portTypeName = portTypeName;
    }

    public QName getName() {
        return name;
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

    public void setWsdlDocument(WSDLModelImpl wsdlDoc) {
        this.wsdlDoc = wsdlDoc;
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
            wsdlDoc.finalizeRpcLitBinding(this);
            finalized = true;
        }
    }

    /**
     * TODO seems like most of this logic(like populating payloadMap should be
     * done much before. Here, it requires only lookup
     *
     * Also payloadMap is not used.
     *
     */
    public WSDLBoundOperation getOperation(String namespaceUri, String localName) {
        boolean emptyPayload = false;
        /**
         * If the style is rpc then the tag name should be
         * same as operation name so return the operation that matches the tag name.
         */
        if(style==Style.RPC) {
            assert (namespaceUri != null && localName != null);
            return bindingOperations.get(new QName(namespaceUri,localName));
        }

        /**
         * For doclit The tag will be the operation that has the same input part descriptor value
         */
        if(namespaceUri==null && localName == null){
            emptyPayload = true;
            if(emptyPayloadOperation != null){
                return emptyPayloadOperation;
            }
        }

        for(WSDLBoundOperationImpl op:bindingOperations.values()){
            QName name = op.getPayloadName();
            //empty payload
            if(name == null && emptyPayload){
                emptyPayloadOperation = op;
                return op;
            }
            
            if (emptyPayload) {
                return emptyPayloadOperation;
            }

            if(name != null && name.equals(new QName(namespaceUri, localName))){
                payloadMap.put(name, op);
                return op;
            }
        }

        //not found, return null
        return null;
    }

    private WSDLBoundOperationImpl emptyPayloadOperation;
    private final QNameMap<WSDLBoundOperationImpl> payloadMap = new QNameMap<WSDLBoundOperationImpl>();

    public SOAPVersion getSOAPVersion(){
        return SOAPVersion.fromHttpBinding(bindingId);
    }

    private Map<QName, WSDLMessageImpl> messages;

    Map<QName, WSDLMessageImpl> getMessages(){
        return messages;
    }
    void freeze(WSDLModelImpl owner) {
        messages = owner.getMessages();
        portType = owner.getPortType(portTypeName);
        portType.freeze(owner);
        // TODO: error check for null. that's an error in WSDL that needs to be reported
        for (WSDLBoundOperationImpl op : bindingOperations.values()) {
            op.freeze(this);
        }
    }
}
