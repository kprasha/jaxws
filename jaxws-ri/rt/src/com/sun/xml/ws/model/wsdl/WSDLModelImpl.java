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

public class WSDLModelImpl implements WSDLModel {
    private Map<QName, Message> messages;
    private Map<QName, PortType> portTypes;
    private Map<QName, BoundPortType> bindings;
    private Map<QName, Service> services;

    public WSDLModelImpl() {
        messages = new HashMap<QName, Message>();
        portTypes = new HashMap<QName, PortType>();
        bindings = new HashMap<QName, BoundPortType>();
        services = new LinkedHashMap<QName, Service>();
    }

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

    public void addBinding(BoundPortType boundPortType){
        bindings.put(boundPortType.getName(), boundPortType);
    }

    public BoundPortType getBinding(QName name){
        return bindings.get(name);
    }

    public void addService(Service svc){
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
        return bindings;
    }

    public Map<QName, Service> getServices(){
        return services;
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
        if(services.isEmpty())
            return null;
        Service service = services.values().iterator().next();
        Iterator<Port> iter = service.getPorts();
        QName port = (iter.hasNext())?iter.next().getName():null;
        return port;
    }

    private Port getFirstPort(){
        if(services.isEmpty())
            return null;
        Service service = services.values().iterator().next();
        Iterator<Port> iter = service.getPorts();
        Port port = iter.hasNext()?iter.next():null;
        return port;
    }


    /**
     * Returns biningId of the first port
     */
    public String getBindingId(){
        Port port = getFirstPort();
        if(port == null)
            return null;
        BoundPortType boundPortType = bindings.get(port.getBindingName());
        if(boundPortType == null)
            return null;
        return boundPortType.getBindingId();
    }

    /**
     * Gives the binding Id of the given service and port
     * @param service
     * @param port
     */
    public String getBindingId(QName service, QName port){
        Service s = services.get(service);
        if(s != null){
            Port p = s.get(port);
            if(p != null){
                BoundPortType b = bindings.get(p.getBindingName());
                if(b != null)
                    return b.getBindingId();
            }

        }
        return null;
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
            if(port != null){
                QName bindingName = port.getBindingName();
                return bindings.get(bindingName);
            }
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
        Iterator<Port> ports = service.getPorts();
        if(!ports.hasNext())
            return bs;
        while(ports.hasNext()){
            Port port  = ports.next();
            BoundPortType b = bindings.get(port.getName());
            if(b == null)
                return bs;
            if(b.equals(bindingId))
                bs.add(b);
        }
        return bs;
    }

    void finalizeBinding(BoundPortType boundPortType){
        assert(boundPortType != null);
        QName portTypeName = boundPortType.getPortTypeName();
        if(portTypeName == null)
            return;
        PortType pt = portTypes.get(portTypeName);
        if(pt == null)
            return;
        Iterator<BoundOperation> boIter = boundPortType.getBindingOperations();
        while(!boIter.hasNext()){
            String op = boIter.next().getName();
            Operation pto = pt.get(op);
            if(pto == null)
                return;
            QName inMsgName = pto.getInputMessage();
            if(inMsgName == null)
                continue;
            Message inMsg = messages.get(inMsgName);
            BoundOperationImpl bo = (BoundOperationImpl) boundPortType.get(op);
            int bodyindex = 0;
            if(inMsg != null){
                for(String name:inMsg){
                    ParameterBinding pb = bo.getInputBinding(name);
                    if(pb.isBody()){
                        bo.addPart(new PartImpl(name, pb, bodyindex++), Mode.IN);
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
                    ParameterBinding pb = bo.getOutputBinding(name);
                    if(pb.isBody()){
                        bo.addPart(new PartImpl(name, pb, bodyindex++), Mode.OUT);
                    }
                }
            }
        }
    }
}
