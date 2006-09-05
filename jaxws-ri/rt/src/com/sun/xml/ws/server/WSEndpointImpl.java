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

package com.sun.xml.ws.server;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.addressing.MemberSubmissionEndpointReference;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.PipelineAssembler;
import com.sun.xml.ws.api.pipe.PipelineAssemblerFactory;
import com.sun.xml.ws.api.pipe.ServerPipeAssemblerContext;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.server.TransportBackChannel;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.server.WebServiceContextDelegate;
import com.sun.xml.ws.fault.SOAPFaultBuilder;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;
import com.sun.xml.ws.addressing.W3CAddressingConstants;
import com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants;
import com.sun.xml.messaging.saaj.util.ByteOutputStream;

import javax.annotation.PreDestroy;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.W3CEndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * {@link WSEndpoint} implementation.
 *
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
public final class WSEndpointImpl<T> extends WSEndpoint<T> {
    private final WSBinding binding;
    private final SEIModel seiModel;
    private final @NotNull Container container;
    private final WSDLPort port;

    private final Pipe masterPipeline;
    private final ServiceDefinitionImpl serviceDef;
    private final SOAPVersion soapVersion;

    /**
     * Set to true once we start shutting down this endpoint.
     * Used to avoid running the clean up processing twice.
     *
     * @see #dispose()
     */
    private boolean disposed;

    private final Class<T> implementationClass;


    public WSEndpointImpl(WSBinding binding, Container container, SEIModel seiModel, WSDLPort port, Class<T> implementationClass, @Nullable ServiceDefinitionImpl serviceDef, InvokerPipe terminalPipe, boolean isSynchronous) {
        this.binding = binding;
        this.soapVersion = binding.getSOAPVersion();
        this.container = container;
        this.port = port;
        this.implementationClass = implementationClass;
        this.serviceDef = serviceDef;
        this.seiModel = seiModel;
        if (serviceDef != null) {
            serviceDef.setOwner(this);
        }

        PipelineAssembler assembler = PipelineAssemblerFactory.create(
                Thread.currentThread().getContextClassLoader(), binding.getBindingId());
        assert assembler!=null;

        ServerPipeAssemblerContext context = new ServerPipeAssemblerContext(seiModel, port, this, terminalPipe, isSynchronous);
        this.masterPipeline = assembler.createServer(context);
        terminalPipe.setEndpoint(this);
    }

    public @NotNull Class<T> getImplementationClass() {
        return implementationClass;
    }

    public @NotNull WSBinding getBinding() {
        return binding;
    }

    public @NotNull Container getContainer() {
        return container;
    }

    public WSDLPort getPort() {
        return port;
    }

    /**
     * Gets the {@link SEIModel} that represents the relationship
     * between WSDL and Java SEI.
     *
     * <p>
     * This method returns a non-null value if and only if this
     * endpoint is ultimately serving an application through an SEI.
     *
     * @return
     *      maybe null. See above for more discussion.
     *      Always the same value.
     */
    public @Nullable SEIModel getSEIModel() {
        return seiModel;
    }


    public @NotNull PipeHead createPipeHead() {
        return new PipeHead() {
            private final Pipe pipe = PipeCloner.clone(masterPipeline);

            public @NotNull Packet process(Packet request, WebServiceContextDelegate wscd, TransportBackChannel tbc) {
                request.webServiceContextDelegate = wscd;
                request.transportBackChannel = tbc;
                request.endpoint = WSEndpointImpl.this;
                Packet response;
                try {
                    response = pipe.process(request);
                } catch (RuntimeException re) {
                    // Catch all runtime exceptions so that transport doesn't
                    // have to worry about converting to wire message
                    // TODO XML/HTTP binding
                    re.printStackTrace();
                    Message faultMsg = SOAPFaultBuilder.createSOAPFaultMessage(
                            soapVersion, null, re);
                    response = request.createResponse(faultMsg);
                }
                return response;
            }
        };
    }

    public synchronized void dispose() {
        if(disposed)
            return;
        disposed = true;

        masterPipeline.preDestroy();

        for (Handler handler : binding.getHandlerChain()) {
            for (Method method : handler.getClass().getMethods()) {
                if (method.getAnnotation(PreDestroy.class) == null) {
                    continue;
                }
                try {
                    method.invoke(handler);
                } catch (Exception e) {
                    logger.warning("exception ignored from handler " +
                        "@PreDestroy method: " +
                        e.getMessage());
                }
                break;
            }
        }
    }

    public ServiceDefinitionImpl getServiceDefinition() {
        return serviceDef;
    }

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.endpoint");

    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz, String address) {
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
