/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.xml.ws.model;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.bind.api.JAXBRIContext;
import com.sun.xml.bind.api.TypeReference;
import com.sun.xml.ws.api.model.MEP;
import com.sun.xml.ws.api.model.ParameterBinding;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.encoding.soap.streaming.SOAPNamespaceConstants;
import com.sun.xml.ws.model.wsdl.WSDLBoundOperationImpl;
import com.sun.xml.ws.model.wsdl.WSDLBoundPortTypeImpl;
import com.sun.xml.ws.model.wsdl.WSDLPartImpl;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;
import com.sun.xml.ws.util.Pool;

import javax.jws.WebParam.Mode;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * model of the web service.  Used by the runtime marshall/unmarshall
 * web service invocations
 *
 * $author: JAXWS Development Team
 */
public abstract class AbstractSEIModelImpl implements SEIModel {

    void postProcess() {
        // should be called only once.
        if (jaxbContext != null)
            return;
        populateMaps();
        populateAsyncExceptions();
        createJAXBContext();
    }

    /**
     * Link {@link SEIModel} to {@link WSDLModel}.
     * Merge it with {@link #postProcess()}.
     */
    void freeze(WSDLPortImpl port) {
        this.port = port;
        for (JavaMethodImpl m : javaMethods) {
            m.freeze(port);
        }
    }

    /**
     * Populate methodToJM and nameToJM maps.
     */
    abstract protected void populateMaps();

    private void populateAsyncExceptions() {
        for (JavaMethodImpl jm : getJavaMethods()) {
            MEP mep = jm.getMEP();
            if (mep.isAsync) {
                String opName = jm.getOperationName();
                Method m = jm.getMethod();
                Class[] params = m.getParameterTypes();
                if (mep == MEP.ASYNC_CALLBACK) {
                    params = new Class[params.length-1];
                    System.arraycopy(m.getParameterTypes(), 0, params, 0, m.getParameterTypes().length-1);
                }
                try {
                    Method om = m.getDeclaringClass().getMethod(opName, params);
                    JavaMethodImpl jm2 = getJavaMethod(om);
                    for (CheckedExceptionImpl ce : jm2.getCheckedExceptions()) {
                        jm.addException(ce);
                    }
                } catch (NoSuchMethodException ex) {
                }
            }
        }
    }

    public Pool.Marshaller getMarshallerPool() {
        return marshallers;
    }

    /**
     * @return the <code>JAXBRIContext</code>
     */
    public JAXBRIContext getJAXBContext() {
        return jaxbContext;
    }

    /**
     * @return the known namespaces from JAXBRIContext
     */
    public List<String> getKnownNamespaceURIs() {
        return knownNamespaceURIs;
    }

    /**
     * @param type
     * @return the <code>Bridge</code> for the <code>type</code>
     */
    public final Bridge getBridge(TypeReference type) {
        Bridge b = bridgeMap.get(type);
        assert b!=null; // we should have created Bridge for all TypeReferences known to this model
        return b;
    }

    private JAXBRIContext createJAXBContext() {
        final List<TypeReference> types = getAllTypeReferences();
        final Class[] cls = new Class[types.size()];
        int i = 0;
        for (TypeReference type : types) {
            cls[i++] = (Class) type.type;
        }
        try {
            //jaxbContext = JAXBRIContext.newInstance(cls, types, targetNamespace, false);
            // Need to avoid doPriv block once JAXB is fixed. Afterwards, use the above
            jaxbContext = AccessController.doPrivileged(new PrivilegedExceptionAction<JAXBRIContext>() {
                public JAXBRIContext run() throws Exception {
                    return JAXBRIContext.newInstance(cls, types, targetNamespace, false);
                }
            });
            createBridgeMap(types);
        } catch (PrivilegedActionException e) {
            throw new WebServiceException(e.getMessage(), e.getException());
        }
        knownNamespaceURIs = new ArrayList<String>();
        for (String namespace : jaxbContext.getKnownNamespaceURIs()) {
            if (namespace.length() > 0) {
                if (!namespace.equals(SOAPNamespaceConstants.XSD))
                    knownNamespaceURIs.add(namespace);
            }
        }

        marshallers = new Pool.Marshaller(jaxbContext);

        return jaxbContext;
    }

    /**
     * @return returns non-null list of TypeReference
     */
    private List<TypeReference> getAllTypeReferences() {
        List<TypeReference> types = new ArrayList<TypeReference>();
        Collection<JavaMethodImpl> methods = methodToJM.values();
        for (JavaMethodImpl m : methods) {
            m.fillTypes(types);
        }
        return types;
    }

    private void createBridgeMap(List<TypeReference> types) {
        for (TypeReference type : types) {
            Bridge bridge = jaxbContext.createBridge(type);
            bridgeMap.put(type, bridge);
        }
    }

    /**
     * @param qname
     * @return the <code>Method</code> for a given WSDLOperation <code>qname</code>
     */
    public Method getDispatchMethod(QName qname) {
        //handle the empty body
        if (qname == null)
            qname = emptyBodyName;
        JavaMethodImpl jm = getJavaMethod(qname);
        if (jm != null) {
            return jm.getMethod();
        }
        return null;
    }

    /**
     * @param name
     * @param method
     * @return true if <code>name</code> is the name
     * of a known fault name for the <code>Method method</code>
     */
    public boolean isKnownFault(QName name, Method method) {
        JavaMethodImpl m = getJavaMethod(method);
        for (CheckedExceptionImpl ce : m.getCheckedExceptions()) {
            if (ce.getDetailType().tagName.equals(name))
                return true;
        }
        return false;
    }

    /**
     * @param m
     * @param ex
     * @return true if <code>ex</code> is a Checked Exception
     * for <code>Method m</code>
     */
    public boolean isCheckedException(Method m, Class ex) {
        JavaMethodImpl jm = getJavaMethod(m);
        for (CheckedExceptionImpl ce : jm.getCheckedExceptions()) {
            if (ce.getExcpetionClass().equals(ex))
                return true;
        }
        return false;
    }

    /**
     * @param method
     * @return the <code>JavaMethod</code> representing the <code>method</code>
     */
    public JavaMethodImpl getJavaMethod(Method method) {
        return methodToJM.get(method);
    }

    /**
     * @param name
     * @return the <code>JavaMethod</code> associated with the
     * operation named name
     */
    public JavaMethodImpl getJavaMethod(QName name) {
        return nameToJM.get(name);
    }

    /**
     * @param jm
     * @return the <code>QName</code> associated with the
     * JavaMethod jm
     */
    public QName getQNameForJM(JavaMethodImpl jm) {
        for (QName key : nameToJM.keySet()) {
            JavaMethodImpl jmethod = nameToJM.get(key);
            if (jmethod.getOperationName().equals(jm.getOperationName())){
               return key;
            }
        }
        return null;
    }

    /**
     * @return a <code>Collection</code> of <code>JavaMethods</code>
     * associated with this <code>RuntimeModel</code>
     */
    public final Collection<JavaMethodImpl> getJavaMethods() {
        return Collections.unmodifiableList(javaMethods);
    }

    void addJavaMethod(JavaMethodImpl jm) {
        if (jm != null)
            javaMethods.add(jm);
    }

    /**
     * Used from {@link WSServiceDelegate}
     * to apply the binding information from WSDL after the model is created frm SEI class on the client side. On the server
     * side all the binding information is available before modeling and this method is not used.
     *
     * @param wsdlBinding
     * @deprecated To be removed once client side new architecture is implemented
     */
    public void applyParameterBinding(WSDLBoundPortTypeImpl wsdlBinding){
        if(wsdlBinding == null)
            return;
        if(wsdlBinding.isRpcLit())
            wsdlBinding.finalizeRpcLitBinding();
        for(JavaMethodImpl method : javaMethods){
            if(method.isAsync())
                continue;
            QName opName = new QName(wsdlBinding.getPortTypeName().getNamespaceURI());
            boolean isRpclit = method.getBinding().isRpcLit();
            List<ParameterImpl> reqParams = method.requestParams;
            List<ParameterImpl> reqAttachParams = null;
            for(ParameterImpl param:reqParams){
                if(param.isWrapperStyle()){
                    if(isRpclit)
                        reqAttachParams = applyRpcLitParamBinding(method, (WrapperParameter)param, wsdlBinding, Mode.IN);
                    continue;
                }
                String partName = param.getPartName();
                if(partName == null)
                    continue;
                ParameterBinding paramBinding = wsdlBinding.getBinding(opName, partName, Mode.IN);
                if(paramBinding != null)
                    param.setInBinding(paramBinding);
            }

            List<ParameterImpl> resAttachParams = null;
            List<ParameterImpl> resParams = method.responseParams;
            for(ParameterImpl param:resParams){
                if(param.isWrapperStyle()){
                    if(isRpclit)
                        resAttachParams = applyRpcLitParamBinding(method, (WrapperParameter)param, wsdlBinding, Mode.OUT);
                    continue;
                }
                //if the parameter is not inout and its header=true then dont get binding from WSDL
//                if(!param.isINOUT() && param.getBinding().isHeader())
//                    continue;
                String partName = param.getPartName();
                if(partName == null)
                    continue;
                ParameterBinding paramBinding = wsdlBinding.getBinding(opName,
                        partName, Mode.OUT);
                if(paramBinding != null)
                    param.setOutBinding(paramBinding);
            }
            if(reqAttachParams != null){
                for(ParameterImpl p : reqAttachParams){
                    method.addRequestParameter(p);
                }
            }
            if(resAttachParams != null){
                for(ParameterImpl p : resAttachParams){
                    method.addResponseParameter(p);
                }
            }

        }
    }



    /**
     * Applies binding related information to the RpcLitPayload. The payload map is populated correctly.
     * @param method
     * @param wrapperParameter
     * @param boundPortType
     * @param mode
     * @return
     *
     * Returns attachment parameters if/any.
     */
    private List<ParameterImpl> applyRpcLitParamBinding(JavaMethodImpl method, WrapperParameter wrapperParameter, WSDLBoundPortTypeImpl boundPortType, Mode mode) {
        QName opName = new QName(boundPortType.getPortTypeName().getNamespaceURI(), method.getOperationName());
        WSDLBoundOperationImpl bo = boundPortType.get(opName);
        Map<Integer, ParameterImpl> bodyParams = new HashMap<Integer, ParameterImpl>();
        List<ParameterImpl> unboundParams = new ArrayList<ParameterImpl>();
        List<ParameterImpl> attachParams = new ArrayList<ParameterImpl>();
        for(ParameterImpl param : wrapperParameter.wrapperChildren){
            String partName = param.getPartName();
            if(partName == null)
                continue;

            ParameterBinding paramBinding = boundPortType.getBinding(opName,
                    partName, mode);
            if(paramBinding != null){
                if(mode == Mode.IN)
                    param.setInBinding(paramBinding);
                else if(mode == Mode.OUT || mode == Mode.INOUT)
                    param.setOutBinding(paramBinding);

                if(paramBinding.isUnbound()){
                        unboundParams.add(param);
                } else if(paramBinding.isAttachment()){
                    attachParams.add(param);
                }else if(paramBinding.isBody()){
                    if(bo != null){
                        WSDLPartImpl p = bo.getPart(param.getPartName(), mode);
                        if(p != null)
                            bodyParams.put(p.getIndex(), param);
                        else
                            bodyParams.put(bodyParams.size(), param);
                    }else{
                        bodyParams.put(bodyParams.size(), param);
                    }
                }
            }

        }
        wrapperParameter.clear();
        for(int i = 0; i <  bodyParams.size();i++){
            ParameterImpl p = bodyParams.get(i);
            wrapperParameter.addWrapperChild(p);
        }

        //add unbounded parts
        for(ParameterImpl p:unboundParams){
            wrapperParameter.addWrapperChild(p);
        }
        return attachParams;
    }


    /**
     * @param name
     * @param jm
     */
    void put(QName name, JavaMethodImpl jm) {
        nameToJM.put(name, jm);
    }

    /**
     * @param method
     * @param jm
     */
    void put(Method method, JavaMethodImpl jm) {
        methodToJM.put(method, jm);
    }

    public String getWSDLLocation() {
        return wsdlLocation;
    }

    void setWSDLLocation(String location) {
        wsdlLocation = location;
    }

    public QName getServiceQName() {
        return serviceName;
    }

    public WSDLPort getPort() {
        return port;
    }

    public QName getPortName() {
        return portName;
    }

    public QName getPortTypeName() {
        return portTypeName;
    }

    void setServiceQName(QName name) {
        serviceName = name;
    }

    void setPortName(QName name) {
        portName = name;
    }

    void setPortTypeName(QName name) {
        portTypeName = name;
    }

    /**
     * This is the targetNamespace for the WSDL containing the PortType
     * definition
     */
    void setTargetNamespace(String namespace) {
        targetNamespace = namespace;
    }

    /**
     * This is the targetNamespace for the WSDL containing the PortType
     * definition
     */
    public String getTargetNamespace() {
        return targetNamespace;
    }

    private ThreadLocal<BridgeContext> bridgeContext = new ThreadLocal<BridgeContext>() {
        protected BridgeContext initialValue() {
            return jaxbContext.createBridgeContext();
        }
    };

    private Pool.Marshaller marshallers;
    protected JAXBRIContext jaxbContext;
    private String wsdlLocation;
    private QName serviceName;
    private QName portName;
    private QName portTypeName;
    private Map<Method,JavaMethodImpl> methodToJM = new HashMap<Method, JavaMethodImpl>();
    private Map<QName,JavaMethodImpl> nameToJM = new HashMap<QName, JavaMethodImpl>();
    private List<JavaMethodImpl> javaMethods = new ArrayList<JavaMethodImpl>();
    private final Map<TypeReference, Bridge> bridgeMap = new HashMap<TypeReference, Bridge>();
    protected final QName emptyBodyName = new QName("");
    private String targetNamespace = "";
    private List<String> knownNamespaceURIs = null;
    private WSDLPortImpl port;
}