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

package com.sun.xml.ws.client.sei;

import com.sun.istack.NotNull;
import com.sun.xml.messaging.saaj.util.ByteOutputStream;
import com.sun.xml.ws.addressing.W3CAddressingConstants;
import com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.addressing.MemberSubmissionEndpointReference;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.MEP;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.RequestContext;
import com.sun.xml.ws.client.ResponseContextReceiver;
import com.sun.xml.ws.client.Stub;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.model.SOAPSEIModel;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.w3caddressing.W3CEndpointReference;
import javax.xml.ws.spi.ServiceDelegate;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * {@link Stub} that handles method invocations
 * through a strongly-typed endpoint interface.
 *
 * @author Kohsuke Kawaguchi
 */
public final class SEIStub extends Stub implements InvocationHandler {
    public SEIStub(ServiceDelegate owner, BindingImpl binding, SOAPSEIModel seiModel, Pipe master) {
        super(master, binding, seiModel.getPort().getAddress());
        this.owner = owner;
        this.seiModel = seiModel;
        this.soapVersion = binding.getSOAPVersion();

        Map<WSDLBoundOperation, SyncMethodHandler> syncs = new HashMap<WSDLBoundOperation, SyncMethodHandler>();

        // fill in methodHandlers.
        // first fill in sychronized versions
        for (JavaMethodImpl m : seiModel.getJavaMethods()) {
            if (!m.getMEP().isAsync) {
                SyncMethodHandler handler = new SyncMethodHandler(this, m);
                syncs.put(m.getOperation(), handler);
                methodHandlers.put(m.getMethod(), handler);
            }
        }

        for (JavaMethodImpl jm : seiModel.getJavaMethods()) {
            SyncMethodHandler sync = syncs.get(jm.getOperation());
            if (jm.getMEP() == MEP.ASYNC_CALLBACK) {
                Method m = jm.getMethod();
                CallbackMethodHandler handler = new CallbackMethodHandler(
                        this, jm, sync, m.getParameterTypes().length - 1);
                methodHandlers.put(m, handler);
            }
            if (jm.getMEP() == MEP.ASYNC_POLL) {
                Method m = jm.getMethod();
                PollingMethodHandler handler = new PollingMethodHandler(this, jm, sync);
                methodHandlers.put(m, handler);
            }
        }
    }

    public final SOAPSEIModel seiModel;

    public final SOAPVersion soapVersion;

    /**
     * The {@link ServiceDelegate} object that owns us.
     */
    public final ServiceDelegate owner;

    /**
     * For each method on the port interface we have
     * a {@link MethodHandler} that processes it.
     */
    private final Map<Method, MethodHandler> methodHandlers = new HashMap<Method, MethodHandler>();

    public Object invoke(Object proxy, Method method, Object[] args) throws WebServiceException, Throwable {
        MethodHandler handler = methodHandlers.get(method);
        if (handler != null) {
            return handler.invoke(proxy, args);
        } else {
            // we handle the other method invocations by ourselves
            try {
                return method.invoke(this, args);
            } catch (IllegalAccessException e) {
                // impossible
                throw new AssertionError(e);
            } catch (IllegalArgumentException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    public final Packet doProcess(Packet request, RequestContext rc, ResponseContextReceiver receiver) {
        return super.process(request, rc, receiver);
    }

    /**
     * Gets the {@link Executor} to be used for asynchronous method invocations.
     * <p/>
     * <p/>
     * Note that the value this method returns may different from invocations
     * to invocations. The caller must not cache.
     *
     * @return always non-null.
     */
    protected final Executor getExecutor() {
        return owner.getExecutor();
    }

    /**
     *
     * @return W3CEndpointReference if WSDL has no Addressing specified,
     *         if Addressing is specified return the type that the wsdl
     *         specifies
     */
    @Override
    public EndpointReference getEndpointReference() {


        if (this.endpointReference != null)
            return this.endpointReference;
        else {
            //TOdo: need to implement this based on whether wsa addressing is specified.-kw
            //
            // QName serviceName = owner.getServiceName();
            String address = seiModel.getPort().getAddress().toString();

            if (this.endpointReference.getClass().isAssignableFrom(W3CEndpointReference.class)) {
                final ByteOutputStream bos = new ByteOutputStream();
                XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(bos);
                try {
                    writer.writeStartDocument();
                    writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_PREFIX,
                            "EndpointReference", W3CAddressingConstants.WSA_NAMESPACE_NAME);
                    writer.writeNamespace(W3CAddressingConstants.WSA_NAMESPACE_PREFIX,
                            W3CAddressingConstants.WSA_NAMESPACE_NAME);
                    writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_PREFIX,
                            W3CAddressingConstants.WSA_ADDRESS_NAME, W3CAddressingConstants.WSA_NAMESPACE_NAME);
                    writer.writeCharacters(address);
                    writer.writeEndElement();
                    //writeW3CMetaData(writer, address, portAddressResolver, resolver);
                    writeW3CMetaData(writer);
                    writer.writeEndElement();
                    writer.writeEndDocument();
                    writer.flush();
                } catch (XMLStreamException e) {
                    throw new WebServiceException(e);
                } catch (IOException e) {
                    throw new WebServiceException(e);
                }
                //System.out.println(bos.toString());
                return new W3CEndpointReference(new StreamSource(bos.newInputStream()));
            } else if (this.endpointReference.getClass().isAssignableFrom(MemberSubmissionEndpointReference.class)) {
                final ByteOutputStream bos = new ByteOutputStream();
                XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(bos);
                try {
                    writer.writeStartDocument();
                    writer.writeStartElement(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                            "EndpointReference", MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
                    writer.writeNamespace(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                            MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
                    writer.writeStartElement(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                            MemberSubmissionAddressingConstants.WSA_ADDRESS_NAME,
                            MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
                    writer.writeCharacters(address);
                    writer.writeEndElement();
                    writeMSMetaData(writer);
                    writer.writeEndElement();
                    writer.writeEndDocument();
                    writer.flush();
                } catch (XMLStreamException e) {
                    throw new WebServiceException(e);
                }
                System.out.println(bos.toString());
                return new MemberSubmissionEndpointReference(new StreamSource(bos.newInputStream()));

            } else {
                throw new WebServiceException(this.endpointReference.getClass() + "is not a recognizable EndpointReference");
            }
        }
    }


    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
        if (this.endpointReference != null)
           if (this.endpointReference.getClass().isAssignableFrom(clazz))
               return (T) this.endpointReference;
           else {
               throw new WebServiceException("TThe current endpointReference is not assignable from" + clazz + "this case is not yet implemented");
           }

        String address = seiModel.getPort().getAddress().toString();

        if (clazz.isAssignableFrom(W3CEndpointReference.class)) {
            final ByteOutputStream bos = new ByteOutputStream();
            XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(bos);
            try {
                writer.writeStartDocument();
                writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_PREFIX,
                        "EndpointReference", W3CAddressingConstants.WSA_NAMESPACE_NAME);
                writer.writeNamespace(W3CAddressingConstants.WSA_NAMESPACE_PREFIX,
                        W3CAddressingConstants.WSA_NAMESPACE_NAME);
                writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_PREFIX,
                        W3CAddressingConstants.WSA_ADDRESS_NAME, W3CAddressingConstants.WSA_NAMESPACE_NAME);
                writer.writeCharacters(address);
                writer.writeEndElement();
                //writeW3CMetaData(writer, address, portAddressResolver, resolver);
                writeW3CMetaData(writer);
                writer.writeEndElement();
                writer.writeEndDocument();
                writer.flush();
            } catch (XMLStreamException e) {
                throw new WebServiceException(e);
            } catch (IOException e) {
                throw new WebServiceException(e);
            }
            //System.out.println(bos.toString());
            return (T) new W3CEndpointReference(new StreamSource(bos.newInputStream()));
        } else if (clazz.isAssignableFrom(MemberSubmissionEndpointReference.class)) {
            final ByteOutputStream bos = new ByteOutputStream();
            XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(bos);
            try {
                writer.writeStartDocument();
                writer.writeStartElement(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                        "EndpointReference", MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
                writer.writeNamespace(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                        MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
                writer.writeStartElement(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                        MemberSubmissionAddressingConstants.WSA_ADDRESS_NAME,
                        MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
                writer.writeCharacters(address);
                writer.writeEndElement();
                writeMSMetaData(writer);
                writer.writeEndElement();
                writer.writeEndDocument();
                writer.flush();
            } catch (XMLStreamException e) {
                throw new WebServiceException(e);
            }
            //System.out.println(bos.toString());
            return (T) new MemberSubmissionEndpointReference(new StreamSource(bos.newInputStream()));
        } else {
            throw new WebServiceException(clazz + "is not a recognizable EndpointReference");
        }
    }


    private void writeW3CMetaData(XMLStreamWriter writer) throws XMLStreamException, IOException {
        final WSDLPort port = seiModel.getPort();
        if (port != null) {
            writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_PREFIX,
                    W3CAddressingConstants.WSA_METADATA_NAME, W3CAddressingConstants.WSA_NAMESPACE_NAME);
            writer.writeNamespace(W3CAddressingConstants.WSA_NAMESPACE_WSDL_PREFIX,
                    W3CAddressingConstants.WSA_NAMESPACE_WSDL_NAME);

            //Write Interface info
            writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_WSDL_PREFIX,
                    W3CAddressingConstants.WSAW_INTERFACENAME_NAME,
                    W3CAddressingConstants.WSA_NAMESPACE_WSDL_NAME);
            QName portType = getPortTypeName(port);
            String portTypePrefix = portType.getPrefix();
            if (portTypePrefix == null || portTypePrefix.equals("")) {
                //TODO check prefix again
                portTypePrefix = "wsns";
            }
            writer.writeNamespace(portTypePrefix, portType.getNamespaceURI());
            writer.writeCharacters(portTypePrefix + ":" + portType.getLocalPart());
            writer.writeEndElement();

            //Write service and Port info
            writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_WSDL_PREFIX,
                    W3CAddressingConstants.WSAW_SERVICENAME_NAME,
                    W3CAddressingConstants.WSA_NAMESPACE_WSDL_NAME);
            QName service = getServiceName(port);
            QName portQN = getPortName(port);
            String servicePrefix = service.getPrefix();
            if (servicePrefix == null || servicePrefix.equals("")) {
                //TODO check prefix again
                servicePrefix = "wsns";
            }
            writer.writeAttribute(W3CAddressingConstants.WSAW_ENDPOINTNAME_NAME, portQN.getLocalPart());
            writer.writeNamespace(servicePrefix, service.getNamespaceURI());
            writer.writeCharacters(servicePrefix + ":" + service.getLocalPart());
            writer.writeEndElement();
            //getServiceDefinition().getPrimary().writeTo(portAddressResolver, resolver, writer);
            writer.writeEndElement();
        }

    }

    private void writeMSMetaData(XMLStreamWriter writer) throws XMLStreamException {
        final WSDLPort port = seiModel.getPort();
        if (port != null) {
            //Write Interface info
            writer.writeStartElement(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                    MemberSubmissionAddressingConstants.WSA_PORTTYPE_NAME,
                    MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
            QName portType = getPortTypeName(port);
            String portTypePrefix = portType.getPrefix();
            if (portTypePrefix == null || portTypePrefix.equals("")) {
                //TODO check prefix again
                portTypePrefix = "wsns";
            }
            writer.writeNamespace(portTypePrefix, portType.getNamespaceURI());
            writer.writeCharacters(portTypePrefix + ":" + portType.getLocalPart());
            writer.writeEndElement();

            //Write service and Port info
            writer.writeStartElement(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                    MemberSubmissionAddressingConstants.WSA_SERVICENAME_NAME,
                    MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
            QName service = getServiceName(port);
            QName portQN = getPortName(port);
            String servicePrefix = service.getPrefix();
            if (servicePrefix == null || servicePrefix.equals("")) {
                //TODO check prefix again
                servicePrefix = "wsns";
            }
            writer.writeAttribute(MemberSubmissionAddressingConstants.WSA_PORTNAME_NAME,
                    portQN.getLocalPart());
            writer.writeNamespace(servicePrefix, service.getNamespaceURI());
            writer.writeCharacters(servicePrefix + ":" + service.getLocalPart());
            writer.writeEndElement();
        }

    }

    private QName getServiceName(@NotNull WSDLPort wsdlport) {
        return wsdlport.getOwner().getName();
    }

    private QName getPortName(@NotNull WSDLPort wsdlport) {
        return wsdlport.getName();
    }

    private QName getPortTypeName(@NotNull WSDLPort wsdlport) {
        return wsdlport.getBinding().getPortTypeName();
    }
}
