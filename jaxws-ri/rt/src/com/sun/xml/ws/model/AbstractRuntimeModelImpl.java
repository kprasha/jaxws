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
package com.sun.xml.ws.model;

import com.sun.xml.ws.pept.presentation.MEP;
import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.bind.api.JAXBRIContext;
import com.sun.xml.bind.api.TypeReference;
import com.sun.xml.bind.api.RawAccessor;
import com.sun.xml.ws.encoding.JAXWSAttachmentMarshaller;
import com.sun.xml.ws.encoding.JAXWSAttachmentUnmarshaller;
import com.sun.xml.ws.encoding.jaxb.JAXBBridgeInfo;
import com.sun.xml.ws.encoding.jaxb.RpcLitPayload;
import com.sun.xml.ws.encoding.soap.streaming.SOAPNamespaceConstants;
import com.sun.xml.ws.wsdl.parser.Binding;
import com.sun.xml.ws.wsdl.parser.Part;
import com.sun.xml.ws.wsdl.parser.BindingOperation;
import com.sun.xml.ws.api.model.JavaMethod;
import com.sun.xml.ws.api.model.CheckedException;
import com.sun.xml.ws.api.model.Parameter;
import com.sun.xml.ws.api.model.RuntimeModel;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * model of the web service.  Used by the runtime marshall/unmarshall 
 * web service invocations
 *
 * $author: JAXWS Development Team
 */
public abstract class AbstractRuntimeModelImpl implements RuntimeModel {

    /**
     *
     */
    public AbstractRuntimeModelImpl() {
        super();
    }

    void postProcess() {
        // should be called only once.
        if (jaxbContext != null)
            return;
        populateMaps();
        populateAsyncExceptions();
        createJAXBContext();
        createDecoderInfo();
    }

    /**
     * Populate methodToJM and nameToJM maps.
     */
    protected void populateMaps() {
        for (JavaMethod jm : getJavaMethods()) {
            put(jm.getMethod(), jm);
            for (Parameter p : jm.getRequestParameters()) {
                put(p.getName(), jm);
            }
        }
    }
    
    protected void populateAsyncExceptions() {
        for (JavaMethod jm : getJavaMethods()) {
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
                    JavaMethod jm2 = getJavaMethod(om);
                    for (CheckedException ce : jm2.getCheckedExceptions()) {
                        ((JavaMethodImpl)jm).addException(ce);
                    }
                } catch (NoSuchMethodException ex) {
                }
            }
        }
    }

    /**
     * @return the <code>BridgeContext</code> for this <code>RuntimeModel</code>
     */
    public BridgeContext getBridgeContext() {
        if (jaxbContext == null)
            return null;
        BridgeContext bc = bridgeContext.get();
        if (bc == null) {
            bc = jaxbContext.createBridgeContext();
            bc.setAttachmentMarshaller(new JAXWSAttachmentMarshaller(enableMtom));
            bc.setAttachmentUnmarshaller(new JAXWSAttachmentUnmarshaller());
            bridgeContext.set(bc);
        }
        return bc;
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
    public Bridge getBridge(TypeReference type) {
        return bridgeMap.get(type);
    }

    /**
     * @param name
     * @return either a <code>RpcLitpayload</code> or a <code>JAXBBridgeInfo</code> for 
     * either a message payload or header
     * @deprecated Will no longer be needed with the {@link com.sun.xml.ws.api.message.Message}
     */
    public Object getDecoderInfo(QName name) {
        Object obj = payloadMap.get(name);
        if (obj instanceof RpcLitPayload) {
            return RpcLitPayload.copy((RpcLitPayload) obj);
        } else if (obj instanceof JAXBBridgeInfo) {
            return JAXBBridgeInfo.copy((JAXBBridgeInfo) obj);
        }
        return null;
    }

    /**
     * @param name Qualified name of the message payload or header
     * @param payload  One of {@link RpcLitPayload} or {@link JAXBBridgeInfo}
     * @deprecated It will be no longer needed with the {@link com.sun.xml.ws.api.message.Message}
     */
    void addDecoderInfo(QName name, Object payload) {
        payloadMap.put(name, payload);
    }

    /**
     * @return
     */
    private JAXBRIContext createJAXBContext() {
        final List<TypeReference> types = getAllTypeReferences();
        final Class[] cls = new Class[types.size()];
        final String ns = targetNamespace;
        int i = 0;
        for (TypeReference type : types) {
            cls[i++] = (Class) type.type;
        }
        try {
            //jaxbContext = JAXBRIContext.newInstance(cls, types, targetNamespace, false);
            // Need to avoid doPriv block once JAXB is fixed. Afterwards, use the above
            jaxbContext = (JAXBRIContext)
                 AccessController.doPrivileged(new PrivilegedExceptionAction() {
                     public java.lang.Object run() throws Exception {
                         return JAXBRIContext.newInstance(cls, types, ns, false);
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

        return jaxbContext;
    }

    /**
     * @return returns non-null list of TypeReference
     */
    private List<TypeReference> getAllTypeReferences() {
        List<TypeReference> types = new ArrayList<TypeReference>();
        Collection<JavaMethod> methods = methodToJM.values();
        for (JavaMethod m : methods) {
            fillTypes(m, types);
            fillFaultDetailTypes(m, types);
        }
        return types;
    }

    private void fillFaultDetailTypes(JavaMethod m, List<TypeReference> types) {
        for (CheckedException ce : m.getCheckedExceptions()) {
            types.add(ce.getDetailType());
//            addGlobalType(ce.getDetailType());
        }
    }

    protected void fillTypes(JavaMethod m, List<TypeReference> types) {
        addTypes(m.getRequestParameters(), types);
        addTypes(m.getResponseParameters(), types);
    }

    private void addTypes(List<Parameter> params, List<TypeReference> types) {
        for (Parameter p : params) {
            types.add(p.getTypeReference());
        }
    }

    private void createBridgeMap(List<TypeReference> types) {
        for (TypeReference type : types) {
            Bridge bridge = jaxbContext.createBridge(type);
            bridgeMap.put(type, bridge);
        }
    }

    /**
     * @param qname
     * @return the <code>Method</code> for a given Operation <code>qname</code>
     */
    public Method getDispatchMethod(QName qname) {
        //handle the empty body
        if (qname == null)
            qname = emptyBodyName;
        JavaMethod jm = getJavaMethod(qname);
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
        JavaMethod m = getJavaMethod(method);
        for (CheckedException ce : m.getCheckedExceptions()) {
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
        JavaMethod jm = getJavaMethod(m);
        for (CheckedException ce : jm.getCheckedExceptions()) {
            if (ce.getExcpetionClass().equals(ex))
                return true;
        }
        return false;
    }

    /**
     * @param method
     * @return the <code>JavaMethod</code> representing the <code>method</code>
     */
    public JavaMethod getJavaMethod(Method method) {
        return methodToJM.get(method);
    }

    /**
     * @param name
     * @return the <code>JavaMethod</code> associated with the 
     * operation named name
     */
    public JavaMethod getJavaMethod(QName name) {
        return nameToJM.get(name);
    }

    /**
     * @param jm
     * @return the <code>QName</code> associated with the
     * JavaMethod jm
     */
    public QName getQNameForJM(JavaMethod jm) {
        Set<QName> set = nameToJM.keySet();
        Iterator iter = set.iterator();
        while (iter.hasNext()){
            QName key = (QName) iter.next();
            JavaMethod jmethod = nameToJM.get(key);
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
    public Collection<JavaMethod> getJavaMethods() {
        return Collections.unmodifiableList(javaMethods);
    }

    void addJavaMethod(JavaMethodImpl jm) {
        if (jm != null)
            javaMethods.add(jm);
    }

    /**
     * Used from {@link com.sun.xml.ws.client.WSServiceDelegate#buildEndpointIFProxy(javax.xml.namespace.QName, Class)}}
     * to apply the binding information from WSDL after the model is created frm SEI class on the client side. On the server
     * side all the binding information is available before modeling and this method is not used.
     *
     * @param wsdlBinding
     * @deprecated To be removed once client side new architecture is implemented
     */
    public void applyParameterBinding(Binding wsdlBinding){
        if(wsdlBinding == null)
            return;
        wsdlBinding.finalizeBinding();
        for(JavaMethod method : javaMethods){
            if(method.isAsync())
                continue;
            boolean isRpclit = ((com.sun.xml.ws.api.model.soap.SOAPBinding)method.getBinding()).isRpcLit();
            List<Parameter> reqParams = method.getRequestParameters();
            List<Parameter> reqAttachParams = null;
            for(Parameter param:reqParams){
                if(param.isWrapperStyle()){
                    if(isRpclit)
                        reqAttachParams = applyRpcLitParamBinding(method, (WrapperParameter)param, wsdlBinding, Mode.IN);
                    continue;
                }
                String partName = param.getPartName();
                if(partName == null)
                    continue;
                ParameterBinding paramBinding = wsdlBinding.getBinding(method.getOperationName(),
                        partName, Mode.IN);
                if(paramBinding != null)
                    ((ParameterImpl)param).setInBinding(paramBinding);
            }

            List<Parameter> resAttachParams = null;
            List<Parameter> resParams = method.getResponseParameters();
            for(Parameter param:resParams){
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
                ParameterBinding paramBinding = wsdlBinding.getBinding(method.getOperationName(),
                        partName, Mode.OUT);
                if(paramBinding != null)
                    ((ParameterImpl)param).setOutBinding(paramBinding);
            }
            if(reqAttachParams != null){
                for(Parameter p : reqAttachParams){
                    ((JavaMethodImpl)method).addRequestParameter(p);
                }
            }
            if(resAttachParams != null){
                for(Parameter p : resAttachParams){
                    ((JavaMethodImpl)method).addResponseParameter(p);
                }
            }

        }
    }



    /**
     * Applies binding related information to the RpcLitPayload. The payload map is populated correctly.
     * @param method
     * @param wrapperParameter
     * @param wsdlBinding
     * @param mode
     * @return
     *
     * Returns attachment parameters if/any.
     */
    private List<Parameter> applyRpcLitParamBinding(JavaMethod method, WrapperParameter wrapperParameter, Binding wsdlBinding, Mode mode) {
        String opName = method.getOperationName();
        RpcLitPayload payload = new RpcLitPayload(wrapperParameter.getName());
        BindingOperation bo = wsdlBinding.get(opName);
        Map<Integer, Parameter> bodyParams = new HashMap<Integer, Parameter>();
        List<Parameter> unboundParams = new ArrayList<Parameter>();
        List<Parameter> attachParams = new ArrayList<Parameter>();
        for(Parameter param:wrapperParameter.getWrapperChildren()){
            String partName = param.getPartName();
            if(partName == null)
                continue;

            ParameterBinding paramBinding = wsdlBinding.getBinding(opName,
                    partName, mode);
            if(paramBinding != null){
                if(mode == Mode.IN)
                    ((ParameterImpl)param).setInBinding(paramBinding);
                else if(mode == Mode.OUT)
                    ((ParameterImpl)param).setOutBinding(paramBinding);

                if(paramBinding.isUnbound()){
                        unboundParams.add(param);
                } else if(paramBinding.isAttachment()){
                    attachParams.add(param);
                }else if(paramBinding.isBody()){
                    if(bo != null){
                        Part p = bo.getPart(param.getPartName(), mode);
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
            Parameter p = bodyParams.get(i);
            wrapperParameter.addWrapperChild(p);
            if(((mode == Mode.IN) && p.getInBinding().isBody())||
                    ((mode == Mode.OUT) && p.getOutBinding().isBody())){
                JAXBBridgeInfo bi = new JAXBBridgeInfo(getBridge(p.getTypeReference()), null);
                payload.addParameter(bi);
            }
        }

        for(Parameter p : attachParams){
            JAXBBridgeInfo bi = new JAXBBridgeInfo(getBridge(p.getTypeReference()), null);
            payloadMap.put(p.getName(), bi);
        }

        //add unbounded parts
        for(Parameter p:unboundParams){
            wrapperParameter.addWrapperChild(p);
        }
        payloadMap.put(wrapperParameter.getName(), payload);
        return attachParams;
    }


    /**
     * @param name
     * @param jm
     */
    void put(QName name, JavaMethod jm) {
        nameToJM.put(name, jm);
    }

    /**
     * @param method
     * @param jm
     */
    void put(Method method, JavaMethod jm) {
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
    
    /**
     * Mtom processing is disabled by default. To enable it the RuntimeModel creator must call it to enable it.
     * Its used by {@link com.sun.xml.ws.server.RuntimeEndpointInfo#init()} after the runtime model is built. This method should not be exposed to outside.
     *
     * This needs to be changed - since this information is available to {@link com.sun.xml.ws.server.RuntimeEndpointInfo}
     * before building the model it should be passed on to {@link com.sun.xml.ws.model.RuntimeModeler#buildRuntimeModel()}.
     *
     * @param enableMtom
     * @deprecated
     */
    public void enableMtom(boolean enableMtom){
        this.enableMtom = enableMtom;
    }

    /**
     * This will no longer be needed with the new architecture
     * @return
     * @deprecated
     */
    public Map<Integer, RawAccessor> getRawAccessorMap() {
        return rawAccessorMap;
    }

    /**
     * This method creates the decoder info that
     * @deprecated
     */
    protected abstract void createDecoderInfo();

    private boolean enableMtom = false;
    private ThreadLocal<BridgeContext> bridgeContext = new ThreadLocal<BridgeContext>();
    protected JAXBRIContext jaxbContext;
    private String wsdlLocation;
    private QName serviceName;
    private QName portName;
    private QName portTypeName;
    private Map<Method, JavaMethod> methodToJM = new HashMap<Method, JavaMethod>();
    private Map<QName, JavaMethod> nameToJM = new HashMap<QName, JavaMethod>();
    private List<JavaMethod> javaMethods = new ArrayList<JavaMethod>();
    private final Map<TypeReference, Bridge> bridgeMap = new HashMap<TypeReference, Bridge>();
    private final Map<QName, Object> payloadMap = new HashMap<QName, Object>();
    protected final QName emptyBodyName = new QName("");
    private String targetNamespace = "";
    private final Map<Integer, RawAccessor> rawAccessorMap = new HashMap<Integer, RawAccessor>();
    private List<String> knownNamespaceURIs = null;
}
