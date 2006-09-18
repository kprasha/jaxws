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
import com.sun.xml.ws.addressing.EndpointReferenceUtil;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Fiber.CompletionCallback;
import com.sun.xml.ws.api.pipe.*;
import com.sun.xml.ws.api.pipe.helper.PipeAdapter;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.server.TransportBackChannel;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.server.WebServiceContextDelegate;
import com.sun.xml.ws.fault.SOAPFaultBuilder;

import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.handler.Handler;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

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
    private final Engine engine;

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
        engine = new Engine();
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

    public void setExecutor(Executor exec) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public void schedule(Packet request, CompletionCallback callback, FiberContextSwitchInterceptor interceptor) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public @NotNull PipeHead createPipeHead() {
        return new PipeHead() {
            private final Pipe pipe = PipeCloner.clone(masterPipeline);
            //private final Tube tube = PipeAdapter.adapt(PipeCloner.clone(masterPipeline));

            public @NotNull Packet process(Packet request, WebServiceContextDelegate wscd, TransportBackChannel tbc) {
                request.webServiceContextDelegate = wscd;
                request.transportBackChannel = tbc;
                request.endpoint = WSEndpointImpl.this;
                Fiber fiber = engine.createFiber();
                Packet response;
                try {
                    //response = fiber.runSync(tube,request);
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
        QName portType = getPortTypeName(port);
        QName service = getServiceName(port);
        QName portQN = getPortName(port);
        return EndpointReferenceUtil.getEndpointReference(clazz, address, service,
                portQN.getLocalPart(),portType);
    }

    private QName getServiceName(WSDLPort wsdlport) {
        if(wsdlport == null)
            return null;
        return wsdlport.getOwner().getName();
    }
    private QName getPortName(WSDLPort wsdlport) {
        if(wsdlport == null)
            return null;
        return wsdlport.getName();
    }
    private QName getPortTypeName(WSDLPort wsdlport) {
        if(wsdlport == null)
            return null;
        return wsdlport.getBinding().getPortTypeName();
    }
}
