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

import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.handler.HandlerResolverImpl;
import com.sun.xml.ws.handler.PortInfoImpl;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.RuntimeModeler;
import com.sun.xml.ws.server.RuntimeContext;
import com.sun.xml.ws.util.HandlerAnnotationInfo;
import com.sun.xml.ws.util.HandlerAnnotationProcessor;
import com.sun.xml.ws.wsdl.WSDLContext;
import org.xml.sax.EntityResolver;

import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * $author: WS Development Team
 */
final class ServiceContext {
    private WSDLContext wsdlContext; //from wsdlParsing

    private Class serviceClass;
    private HandlerResolverImpl handlerResolver;
    private QName serviceName; //supplied on creation of service
    /**
     * Service endpoint interface keyed by their interface type.
     */
    private final Map<Class,EndpointIFContext> seiContext = new HashMap<Class,EndpointIFContext>();
    /**
     * To be used to resolve WSDL resources.
     */
    private final EntityResolver entityResolver;
    private HashMap<QName,Set<String>> rolesMap = new HashMap<QName,Set<String>>();


    public ServiceContext(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    public WSDLContext getWsdlContext() {
        return wsdlContext;
    }

    public void setWsdlContext(WSDLContext wsdlContext) {
        this.wsdlContext = wsdlContext;
    }

    public WSDLModel getWSDLModel(){
        return (wsdlContext != null)?wsdlContext.getWSDLModel():null;
    }

    public HandlerResolverImpl getHandlerResolver() {
        return handlerResolver;
    }

    public void setHandlerResolver(HandlerResolverImpl resolver) {
        this.handlerResolver = resolver;
    }

    public Set<String> getRoles(QName portName) {
        return rolesMap.get(portName);
    }

    public void setRoles(QName portName,Set<String> roles) {
        rolesMap.put(portName,roles);
    }

    public EndpointIFContext getEndpointIFContext(Class sei) {
        return seiContext.get(sei);
    }

    public boolean hasSEI() {
        return !seiContext.isEmpty();
    }

    private void addEndpointIFContext(EndpointIFContext eifContext) {
        this.seiContext.put(eifContext.getSei(),eifContext);
    }

    public void setServiceClass(Class serviceClass) {
        this.serviceClass = serviceClass;
    }

    public QName getServiceName() {
        if (serviceName == null) {
            if (wsdlContext != null) {
                setServiceName(wsdlContext.getFirstServiceName());
            }
        }
        return serviceName;
    }

    public void setServiceName(QName serviceName) {
        assert(serviceName != null);
        this.serviceName = serviceName;
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    //todo: valid port in wsdl
    void addPort(Class portInterface) throws WebServiceException {
        EndpointIFContext eifc = getEndpointIFContext(portInterface);
        if ((eifc == null) || (eifc.getRuntimeContext() == null)) {

            if (eifc == null) {
                eifc = new EndpointIFContext(portInterface);
                addEndpointIFContext(eifc);
            }

            //toDo:
            QName serviceName = getServiceName();
            QName portName = eifc.getPortName();
            if (serviceClass != null) {
                if (serviceName == null)
                    serviceName = getServiceName(serviceClass);
                if (portName == null)
                    portName = getPortName(portInterface, serviceClass);
            }

            if (portName == null) {
                portName = wsdlContext.getPortName();
            }

            //todo:use SCAnnotations and put in map
            String bindingId = wsdlContext.getBindingID(
                serviceName, portName);
            RuntimeModeler modeler = new RuntimeModeler(portInterface,
                serviceName, bindingId);
            modeler.setPortName(portName);
            AbstractSEIModelImpl model = modeler.buildRuntimeModel();

            eifc.setRuntimeContext(new RuntimeContext(model));

            // get handler information
            HandlerAnnotationInfo chainInfo =
                HandlerAnnotationProcessor.buildHandlerInfo(portInterface,
                    model.getServiceQName(), model.getPortName(), bindingId);

            if (chainInfo != null) {
                if(handlerResolver==null)
                    handlerResolver = new HandlerResolverImpl();
                handlerResolver.setHandlerChain(new PortInfoImpl(
                    bindingId,
                    model.getPortName(),
                    model.getServiceQName()),
                    chainInfo.getHandlers());
                setRoles(portName,chainInfo.getRoles());

            }
        }
    }

    private static QName getServiceName(Class<?> serviceInterface) {
        WebServiceClient wsClient = serviceInterface.getAnnotation(WebServiceClient.class);
        QName serviceName = null;
        if (wsClient != null) {
            String name = wsClient.name();
            String namespace = wsClient.targetNamespace();
            serviceName = new QName(namespace, name);
        }
        return serviceName;
    }

    private static QName getPortName(Class<?> portInterface, Class<?> serviceInterface) {
        QName portName = null;
        WebServiceClient wsClient = serviceInterface.getAnnotation(WebServiceClient.class);
        for (Method method : serviceInterface.getMethods()) {
            if (method.getDeclaringClass()!=serviceInterface) {
                continue;
            }
            WebEndpoint webEndpoint = method.getAnnotation(WebEndpoint.class);
            if (webEndpoint == null) {
                continue;
            }
            if (method.getGenericReturnType()==portInterface) {
                if (method.getName().startsWith("get")) {
                    portName = new QName(wsClient.targetNamespace(), webEndpoint.name());
                    break;
                }
            }
        }
        return portName;
    }


    public String toString() {
        return "ServiceContext{" +
            "wsdlContext=" + wsdlContext +
            ", handleResolver=" + handlerResolver +
            ", serviceClass=" + serviceClass +
            ", serviceName=" + serviceName +
            ", seiContext=" + seiContext +
            ", entityResolver=" + entityResolver +
            "}";
    }
}
