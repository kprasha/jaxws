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
import com.sun.xml.ws.api.model.Mode;
import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.api.model.wsdl.PortType;
import com.sun.xml.ws.api.model.wsdl.BoundPortType;
import com.sun.xml.ws.api.model.wsdl.Operation;
import com.sun.xml.ws.api.model.wsdl.Port;
import com.sun.xml.ws.api.model.wsdl.Service;
import com.sun.xml.ws.api.model.wsdl.BoundOperation;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * Implementation of {@link WSDLModel}
 *
 * @author Vivek Pandey
 */
public final class WSDLModelImpl implements WSDLModel {
    private final Map<QName, Message> messages = new HashMap<QName, Message>();
    private final Map<QName, PortType> portTypes = new HashMap<QName, PortType>();
    private final Map<QName, BoundPortTypeImpl> bindings = new HashMap<QName, BoundPortTypeImpl>();
    private final Map<QName, ServiceImpl> services = new LinkedHashMap<QName, ServiceImpl>();

    private final Map<QName,BoundPortType> unmBindings
        = Collections.<QName,BoundPortType>unmodifiableMap(bindings);

    public void addMessage(Message msg){
        messages.put(msg.getName(), msg);
    }

    public Message getMessage(QName name){
        return messages.get(name);
    }

    public void addPortType(PortType pt){
        portTypes.put(pt.getName(), pt);
    }

    public PortType getPortType(QName name){
        return portTypes.get(name);
    }

    public void addBinding(BoundPortTypeImpl boundPortType){
        bindings.put(boundPortType.getName(), boundPortType);
    }

    public BoundPortType getBinding(QName name){
        return bindings.get(name);
    }

    public void addService(ServiceImpl svc){
        services.put(svc.getName(), svc);
    }

    public Service getService(QName name){
        return services.get(name);
    }

    public Map<QName, Message> getMessages() {
        return messages;
    }

    public Map<QName, PortType> getPortTypes() {
        return portTypes;
    }

    public Map<QName, BoundPortType> getBindings() {
        return unmBindings;
    }

    public Map<QName, ServiceImpl> getServices(){
        return services;
    }

    //TODO Partial impl
    public BoundOperation getOperation(QName serviceName, QName portName, QName tag) {
        Service service = getService(serviceName);
        if(service  == null)
            return null;
        Port port = service.get(portName);
        if(port == null)
            return null;
        BoundPortType bpt = port.getBinding();
        if(bpt == null)
            return null;
        PortTypeImpl pt = (PortTypeImpl) bpt.getPortType();
        if(pt == null)
            return null;

        for(Operation op: pt.getOperations()){
            QName msgName = op.getInputMessage();
            Message msg = messages.get(msgName);
            //TODO
        }
        return bpt.get(tag);
    }

    /**
     * Returns the first service QName from insertion order
     */
    public QName getFirstServiceName(){
        if(services.isEmpty())
            return null;
        return services.values().iterator().next().getName();
    }

    /**
     * Returns first port QName from first service as per the insertion order
     */
    public QName getFirstPortName(){
        Port fp = getFirstPort();
        if(fp==null)
            return null;
        else
            return fp.getName();
    }

    private Port getFirstPort(){
        if(services.isEmpty())
            return null;
        Service service = services.values().iterator().next();
        Iterator<? extends Port> iter = service.getPorts().iterator();
        Port port = iter.hasNext()?iter.next():null;
        return port;
    }


    /**
     * Returns biningId of the first port
     */
    @Deprecated   // is this method still in use?
    public String getBindingId(){
        Port port = getFirstPort();
        if(port == null)
            return null;
        BoundPortType boundPortType = port.getBinding();
        if(boundPortType == null)
            return null;
        return boundPortType.getBindingId();
    }

     /**
     *
     * @param serviceName non-null service QName
     * @param portName    non-null port QName
     * @return
     *          BoundOperation on success otherwise null. throws NPE if any of the parameters null
     */
    public BoundPortType getBinding(QName serviceName, QName portName){
        Service service = services.get(serviceName);
        if(service != null){
            Port port = service.get(portName);
            if(port != null)
                return port.getBinding();
        }
        return null;
    }

    /**
     * Returns the bindings for the given bindingId
     * @param service  non-null service
     * @param bindingId  non-null binding id
     */
    public List<BoundPortType> getBindings(Service service, String bindingId){
        List<BoundPortType> bs = new ArrayList<BoundPortType>();
        for (Port port : service.getPorts()) {
            BoundPortTypeImpl b = bindings.get(port.getName());
            if(b == null)
                return bs;
            if(b.equals(bindingId))
                bs.add(b);
        }
        return bs;
    }

    private BoundOperation getBoundOperation(BoundPortType bpt){
        PortTypeImpl pt = (PortTypeImpl) bpt.getPortType();
        if(pt == null)
            return null;
        for(Operation op: pt.getOperations()){
            QName msgName = op.getInputMessage();
            Message msg = messages.get(msgName);
        }
        return null;
    }

    void finalizeRpcLitBinding(BoundPortTypeImpl boundPortType){
        assert(boundPortType != null);
        QName portTypeName = boundPortType.getPortTypeName();
        if(portTypeName == null)
            return;
        PortType pt = portTypes.get(portTypeName);
        if(pt == null)
            return;
        for (BoundOperationImpl bop : boundPortType.getBindingOperations()) {
            Operation pto = pt.get(bop.getName().getLocalPart());
            QName inMsgName = pto.getInputMessage();
            if(inMsgName == null)
                continue;
            Message inMsg = messages.get(inMsgName);
            int bodyindex = 0;
            if(inMsg != null){
                for(String name:inMsg){
                    ParameterBinding pb = bop.getInputBinding(name);
                    if(pb.isBody()){
                        bop.addPart(new PartImpl(name, pb, bodyindex++), Mode.IN);
                    }
                }
            }
            bodyindex=0;
            QName outMsgName = pto.getOutputMessage();
            if(outMsgName == null)
                continue;
            Message outMsg = messages.get(outMsgName);
            if(outMsg!= null){
                for(String name:outMsg){
                    ParameterBinding pb = bop.getOutputBinding(name);
                    if(pb.isBody()){
                        bop.addPart(new PartImpl(name, pb, bodyindex++), Mode.OUT);
                    }
                }
            }
        }
    }

    /**
     * Invoked at the end of the model construction to fix up references, etc.
     */
    public void freeze() {
        for (ServiceImpl service : services.values()) {
            service.freeze(this);
        }
        for (BoundPortTypeImpl bp : bindings.values()) {
            bp.freeze(this);
        }
    }
}