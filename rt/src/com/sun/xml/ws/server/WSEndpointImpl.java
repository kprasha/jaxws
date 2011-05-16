/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.ws.server;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.ws.addressing.EPRSDDocumentFilter;
import com.sun.xml.ws.addressing.WSEPRExtension;
import com.sun.xml.ws.addressing.WsaServerTube;
import com.sun.xml.ws.api.*;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.*;
import com.sun.xml.ws.api.server.*;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.AsyncInvoker;
import com.sun.xml.ws.client.AsyncResponseImpl;
import com.sun.xml.ws.client.Stub;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.fault.SOAPFaultBuilder;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;
import com.sun.xml.ws.model.wsdl.WSDLProperties;
import com.sun.xml.ws.model.wsdl.WSDLServiceImpl;
import com.sun.xml.ws.policy.PolicyMap;
import com.sun.xml.ws.resources.HandlerMessages;
import com.sun.xml.ws.util.Pool;
import com.sun.xml.ws.util.Pool.TubePool;
import com.sun.xml.ws.util.ServiceFinder;
import com.sun.xml.ws.wsdl.OperationDispatcher;
import org.glassfish.gmbal.ManagedObjectManager;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.*;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link WSEndpoint} implementation.
 * 
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
public class WSEndpointImpl<T> extends WSEndpoint<T> {
	private static final Logger LOGGER = Logger.getLogger(WSEndpointImpl.class.getName());

    private final @NotNull QName serviceName;
    private final @NotNull QName portName;
	private final WSBinding binding;
	private final SEIModel seiModel;
    private final @NotNull Container container;
	private final @NotNull EndpointFactory owner;
	private final WSDLPort port;

	private WSServiceDelegate delegate;

	private final Tube masterTubeline;
	private final Tube postSpliceTubeline;
	private final ServiceDefinitionImpl serviceDef;
	private final SOAPVersion soapVersion;
	private final Engine engine;
    private final @NotNull Codec masterCodec;
    private final @NotNull PolicyMap endpointPolicy;
	private final Pool<Tube> tubePool;
    private final OperationDispatcher operationDispatcher;
    private final @NotNull ManagedObjectManager managedObjectManager;
    private       boolean managedObjectManagerClosed = false;
    private final @NotNull ServerTubeAssemblerContext context;

    private Map<QName, WSEndpointReference.EPRExtension> endpointReferenceExtensions = new HashMap<QName, WSEndpointReference.EPRExtension>();
	/**
     * Set to true once we start shutting down this endpoint.
     * Used to avoid running the clean up processing twice.
	 * 
	 * @see #dispose()
	 */
	private boolean disposed;

	private final Class<T> implementationClass;
    private final @Nullable WSDLProperties wsdlProperties;
	private final Map<Class<? extends Component>, Component> componentRegistry = new IdentityHashMap<Class<? extends Component>, Component>();
	private final Set<BoundEndpoint> boundEndpoints = new LinkedHashSet<BoundEndpoint>();

    protected WSEndpointImpl(@NotNull QName serviceName, @NotNull QName portName, WSBinding binding,
                   EndpointFactory owner, Container container, SEIModel seiModel, WSDLPort port,
                   Class<T> implementationClass,
                   @Nullable ServiceDefinitionImpl serviceDef,
                   EndpointAwareTube terminalTube, boolean isSynchronous,
                   PolicyMap endpointPolicy) {
		this.serviceName = serviceName;
		this.portName = portName;
		this.binding = binding;
		this.soapVersion = binding.getSOAPVersion();
		this.container = container;
		this.owner = owner;
		this.port = port;
		this.implementationClass = implementationClass;
		this.serviceDef = serviceDef;
		this.seiModel = seiModel;
        this.endpointPolicy = endpointPolicy;

        this.managedObjectManager = 
            new MonitorRootService(this).createManagedObjectManager(this);

		if (serviceDef != null) {
			serviceDef.setOwner(this);
		}

        TubelineAssembler assembler = TubelineAssemblerFactory.create(
                Thread.currentThread().getContextClassLoader(), binding.getBindingId(), container);
		assert assembler != null;

        this.operationDispatcher = (port == null) ? null : new OperationDispatcher(port, binding, seiModel);

        context = createServerTubeAssemblerContext(terminalTube, isSynchronous);
    this.masterTubeline = assembler.createServer(context);
		this.postSpliceTubeline = null;

		Codec c = context.getCodec();
		if (c instanceof EndpointAwareCodec) {
            // create a copy to avoid sharing the codec between multiple endpoints 
			c = c.copy();
			((EndpointAwareCodec) c).setEndpoint(this);
		}
		this.masterCodec = c;

		tubePool = new TubePool(masterTubeline);
		terminalTube.setEndpoint(this);
		engine = new Engine(toString());
		wsdlProperties = (port == null) ? null : new WSDLProperties(port);

        Map<QName, WSEndpointReference.EPRExtension> eprExtensions = new HashMap<QName, WSEndpointReference.EPRExtension>();
        try {
            if (port != null) {
                //gather EPR extrensions from WSDL Model
                WSEndpointReference wsdlEpr = ((WSDLPortImpl) port).getEPR();
                if (wsdlEpr != null) {
                    for (WSEndpointReference.EPRExtension extnEl : wsdlEpr.getEPRExtensions()) {
                        eprExtensions.put(extnEl.getQName(), extnEl);
                    }
                }
            }

            EndpointReferenceExtensionContributor[] eprExtnContributors = ServiceFinder.find(EndpointReferenceExtensionContributor.class).toArray();
            for(EndpointReferenceExtensionContributor eprExtnContributor :eprExtnContributors) {
                WSEndpointReference.EPRExtension wsdlEPRExtn = eprExtensions.remove(eprExtnContributor.getQName());
                    WSEndpointReference.EPRExtension endpointEprExtn = eprExtnContributor.getEPRExtension(this,wsdlEPRExtn);
                    if (endpointEprExtn != null) {
                        eprExtensions.put(endpointEprExtn.getQName(), endpointEprExtn);
                    }
            }
            for (WSEndpointReference.EPRExtension extn : eprExtensions.values()) {
                endpointReferenceExtensions.put(extn.getQName(), new WSEPRExtension(
                        XMLStreamBuffer.createNewBufferFromXMLStreamReader(extn.readAsXMLStreamReader()),extn.getQName()));
            }
        } catch (XMLStreamException ex) {
            throw new WebServiceException(ex);
        }
        if(!eprExtensions.isEmpty()) {
            serviceDef.addFilter(new EPRSDDocumentFilter(this));
        }

    WSEndpoint.registerEndpoint(getEndpointId(), this);
  }

  protected ServerTubeAssemblerContext createServerTubeAssemblerContext(
    	    EndpointAwareTube terminalTube, boolean isSynchronous) {
    ServerTubeAssemblerContext context = new ServerPipeAssemblerContext(
        seiModel, port, this, terminalTube, isSynchronous);
    return context;
  }

  protected WSEndpointImpl(WSEndpoint<T> endpoint,
			EndpointFactory owner, Tube masterTubeline) {
		this(endpoint, owner, null, masterTubeline, null);
	}
	
	protected WSEndpointImpl(WSEndpoint<T> endpoint,
			EndpointFactory owner, SEIModel seiModel, Tube masterTubeline, Tube postSpliceTubeline) {
		this(endpoint.getServiceName(), endpoint.getPortName(), endpoint.getBinding(), endpoint.getContainer(), owner, seiModel, endpoint.getPort(), masterTubeline, postSpliceTubeline);
	}
	
	protected WSEndpointImpl(@NotNull QName serviceName, @NotNull QName portName, WSBinding binding, Container container,
			EndpointFactory owner, SEIModel seiModel, WSDLPort port,
			Tube masterTubeline) {
		this(serviceName, portName, binding, container, owner, seiModel, port, masterTubeline, null);
	}
	
	protected WSEndpointImpl(@NotNull QName serviceName, @NotNull QName portName, WSBinding binding, Container container,
			EndpointFactory owner, SEIModel seiModel, WSDLPort port,
			Tube masterTubeline, Tube postSpliceTubeline) {
		this.serviceName = serviceName;
		this.portName = portName;
		this.binding = binding;
		this.soapVersion = binding.getSOAPVersion();
		this.container = container;
		this.owner = owner;
		this.endpointPolicy = null;
		this.port = port;
		this.seiModel = seiModel;
		this.serviceDef = null;
		this.implementationClass = null;
		this.masterTubeline = masterTubeline;
		this.postSpliceTubeline = postSpliceTubeline;
		this.masterCodec = ((BindingImpl) this.binding).createCodec();

        this.managedObjectManager = 
            new MonitorRootService(this).createManagedObjectManager(this);
        this.operationDispatcher = (port == null) ? null : new OperationDispatcher(port, binding, seiModel);
	    this.context = new ServerPipeAssemblerContext(
    	        seiModel, port, this, null /* not known */, false);

		tubePool = new TubePool(masterTubeline);
		engine = new Engine(toString());
		wsdlProperties = (port == null) ? null : new WSDLProperties(port);

    WSEndpoint.registerEndpoint(getEndpointId(), this);
  }

    public Collection<WSEndpointReference.EPRExtension> getEndpointReferenceExtensions() {
        return endpointReferenceExtensions.values();
    }
    
    /**
     * Nullable when there is no associated WSDL Model
     * @return
     */
    public @Nullable OperationDispatcher getOperationDispatcher() {
        return operationDispatcher;
    }

    public PolicyMap getPolicyMap() {
            return endpointPolicy;
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

	@Override
    public @Nullable SEIModel getSEIModel() {
		return seiModel;
	}

	public void setExecutor(Executor exec) {
		engine.setExecutor(exec);
	}

    public void schedule(final Packet request, final CompletionCallback callback, FiberContextSwitchInterceptor interceptor) {
        processAsync(request, callback, interceptor, true);
    }

    private void processAsync(final Packet request, final CompletionCallback callback, FiberContextSwitchInterceptor interceptor, boolean schedule) {
		request.endpoint = WSEndpointImpl.this;
		if (wsdlProperties != null) {
			request.addSatellite(wsdlProperties);
		} else {
        	//bug8237542 put the properties as invocationProperties. 
        	//REVIEW: how much it will impact the performance? w/o satallite propertySet
        	request.invocationProperties.put(javax.xml.ws.handler.MessageContext.WSDL_SERVICE, serviceName);
        	request.invocationProperties.put(javax.xml.ws.handler.MessageContext.WSDL_PORT, portName);
            //bug9275110 
        	if (seiModel != null) {
        		request.invocationProperties.put(javax.xml.ws.handler.MessageContext.WSDL_DESCRIPTION, 
        			new InputSource(seiModel.getWSDLLocation()));
        	}
        }
        //put the operation name for each invocation.
        String localPart = port == null ? null : request.getMessage().getPayloadLocalPart();
        if (localPart != null) {
            request.invocationProperties.put(javax.xml.ws.handler.MessageContext.WSDL_OPERATION,
                    new QName(request.getMessage().getPayloadNamespaceURI(), localPart));
        }
        Fiber fiber = engine.createFiber();
		if (interceptor != null) {
			fiber.addInterceptor(interceptor);
		}
		final Tube tube = tubePool.take();
		Fiber.CompletionCallback fcc = new Fiber.CompletionCallback() {
            public void onCompletion(@NotNull Packet response) {
				tubePool.recycle(tube);
				if (callback != null) {
					callback.onCompletion(response);
				}
			}

            public void onCompletion(@NotNull Throwable error) {
                // let's not reuse tubes as they might be in a wrong state, so not
				// calling tubePool.recycle()
				error.printStackTrace();
                // Convert all runtime exceptions to Packet so that transport doesn't
				// have to worry about converting to wire message
				// TODO XML/HTTP binding
				Message faultMsg = SOAPFaultBuilder.createSOAPFaultMessage(
						soapVersion, null, error);
                Packet response = request.createServerResponse(faultMsg, request.endpoint.getPort(), null,
                        request.endpoint.getBinding());
				if (callback != null) {
					callback.onCompletion(response);
				}
			}
		};
		if (binding.isFeatureEnabled(SyncStartForAsyncFeature.class)) 
			fiber.startSync(tube, request, fcc);
		else if (schedule) {
            fiber.start(tube, request, fcc);
        } else {
            fiber.runAsync(tube, request, fcc);
        }
	}

    @Override
    public void process(final Packet request, final CompletionCallback callback, FiberContextSwitchInterceptor interceptor) {
        processAsync(request, callback, interceptor, false);
    }

    public @NotNull PipeHead createPipeHead() {
		return new PipeHead() {
			private final Tube tube = TubeCloner.clone(masterTubeline);

            public @NotNull Packet process(Packet request, WebServiceContextDelegate wscd, TransportBackChannel tbc) {
				request.webServiceContextDelegate = wscd;
				request.transportBackChannel = tbc;
				request.endpoint = WSEndpointImpl.this;
				if (wsdlProperties != null) {
					request.addSatellite(wsdlProperties);
				} else {
		        	//bug8237542 put the properties as invocationProperties. 
		        	//REVIEW: how much it will impact the performance? w/o satallite propertySet
		        	request.invocationProperties.put(javax.xml.ws.handler.MessageContext.WSDL_SERVICE, serviceName);
		        	request.invocationProperties.put(javax.xml.ws.handler.MessageContext.WSDL_PORT, portName);
		            //bug9275110
		        	if (seiModel != null) {
		        		request.invocationProperties.put(javax.xml.ws.handler.MessageContext.WSDL_DESCRIPTION, 
	            			new InputSource(seiModel.getWSDLLocation()));
		        	}
				}
		        //put the operation name for each invocation.
                String localPart = port == null ? null : request.getMessage().getPayloadLocalPart();
                if (localPart != null) {
                    request.invocationProperties.put(javax.xml.ws.handler.MessageContext.WSDL_OPERATION,
                            new QName(request.getMessage().getPayloadNamespaceURI(), localPart));
                }

				Fiber fiber = engine.createFiber();
				Packet response;
				try {
					response = fiber.runSync(tube, request);
				} catch (RuntimeException re) {
					// Catch all runtime exceptions so that transport doesn't
					// have to worry about converting to wire message
					// TODO XML/HTTP binding
                    re.printStackTrace();
					Message faultMsg = SOAPFaultBuilder.createSOAPFaultMessage(
							soapVersion, null, re);
                    response = request.createServerResponse(faultMsg, request.endpoint.getPort(), null, request.endpoint.getBinding());
				}
				return response;
			}
		};
	}

	public synchronized void dispose() {
		if (disposed)
			return;
		disposed = true;

    WSEndpoint.unregisterEndpoint(getEndpointId());

    masterTubeline.preDestroy();

		for (Handler handler : binding.getHandlerChain()) {
			for (Method method : handler.getClass().getMethods()) {
				if (method.getAnnotation(PreDestroy.class) == null) {
					continue;
				}
				try {
					method.invoke(handler);
				} catch (Exception e) {
                    logger.log(Level.WARNING, HandlerMessages.HANDLER_PREDESTROY_IGNORE(e.getMessage()), e);
				}
				break;
			}
		}
        closeManagedObjectManager();
	}

	public ServiceDefinitionImpl getServiceDefinition() {
		return serviceDef;
	}

	public Map<Class<? extends Component>, Component> getComponentRegistry() {
		return componentRegistry;
	}
	
	public Set<BoundEndpoint> getBoundEndpoints() {
		return boundEndpoints;
	}
	
	@SuppressWarnings("hiding")
	public @Nullable <T> T getSPI(@NotNull Class<T> spiType) {
		for (Component c : componentRegistry.values()) {
			T t = c.getSPI(spiType);
			if (t != null)
				return t;
		}
		return null;
	}

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.endpoint");

    public <T extends EndpointReference> T getEndpointReference(Class<T>
            clazz, String address, String wsdlAddress, Element... referenceParameters) {
        List<Element> refParams = null;
        if (referenceParameters != null) {
            refParams = Arrays.asList(referenceParameters);
        }
        return getEndpointReference(clazz, address, wsdlAddress, null, refParams);
    }

	public <T extends EndpointReference> T getEndpointReference(
			Class<T> clazz, String address, String wsdlAddress, List<Element> metadata, List<Element> referenceParameters) {
		return getWSEndpointReference(clazz, address, wsdlAddress, metadata, referenceParameters).toSpec(clazz);
  }

	public <T extends EndpointReference> WSEndpointReference getWSEndpointReference(
			Class<T> clazz, String address, String wsdlAddress, List<Element> metadata, List<Element> referenceParameters) {
      QName portType = null;
      if(port != null) {
          portType = port.getBinding().getPortTypeName();
      }

      AddressingVersion av = AddressingVersion.fromSpecClass(clazz);
      if (av == AddressingVersion.W3C) {
            // Suppress writing ServiceName and EndpointName in W3C EPR,
          // Until the ns for those metadata elements is resolved.
          return new WSEndpointReference(
                  AddressingVersion.W3C,
                    address, null /*serviceName*/,null /*portName*/, null /*portType*/, metadata, null /*wsdlAddress*/, referenceParameters);
      } else {
          return new WSEndpointReference(
                  AddressingVersion.MEMBER,
                    address, serviceName, portName, portType, metadata, wsdlAddress, referenceParameters);
      }
  }

    public @NotNull QName getPortName() {
		return portName;
	}


    public @NotNull Codec createCodec() {
		return masterCodec.copy();
	}

    public @NotNull QName getServiceName() {
		return serviceName;
	}

    public @NotNull ManagedObjectManager getManagedObjectManager() {
        return managedObjectManager;
    }

    // This can be called independently of WSEndpoint.dispose.
    // Example: the WSCM framework calls this before dispose.
    public void closeManagedObjectManager() {
        if (managedObjectManagerClosed == true) {
            return;
        }
        MonitorBase.closeMOM(managedObjectManager);
        managedObjectManagerClosed = true;
    }

    public @NotNull ServerTubeAssemblerContext getAssemblerContext() {
        return context;
    }

	@SuppressWarnings("hiding")
	public <T> Dispatch<T> createDispatch(Class<T> type, Mode mode) {
		QName portName = getPortName();
		WSServiceDelegate delegate = getDelegate();
		return Stubs.createDispatch(portName, delegate, binding, type, mode,
				new PartialForwardTube(masterTubeline, postSpliceTubeline), null);
	}

	@SuppressWarnings("hiding")
	public <T> Dispatch<T> createResponseDispatch(Class<T> type, Mode mode,
			@Nullable
			WSEndpointReference epr) {
		QName portName = getPortName();
		WSServiceDelegate delegate = getDelegate();
		if (epr != null)
			delegate.setPortEpr(portName, epr);
    // Create a response tubeline containing all the pre-splice tubes. The
    // ResponseOnlyTube we create will have these tubes via a collaboration
    // between ContinuationCloner and TubelineSpliceMarkerTube. Any splice
    // point lays down a TubelineSpliceMarkerTube just after itself in the
    // original tubeline. ContinuationCloner tries to keep on cloning the whole
    // tubeline, but TubelineSpliceMarkerTube halts the clone. We tell
    // the cloning process to include the tube right before the splice by
    // creating ResponseOnlyTube with isLead==false
    ResponseOnlyTube rot =
      new ResponseOnlyTube(masterTubeline, null, false /* isLead */, true);
		Dispatch<T> stub = Stubs.createDispatch(portName, delegate, binding, type, mode,
				rot, epr);
		((Stub) stub).setServerResponse(true);
        stub.getRequestContext().put(WsaServerTube.SERVER_ENDPOINT_ASYNC_RESPONSE,"true");
        return stub;
	}

	private synchronized WSServiceDelegate getDelegate() {
		if (delegate == null)
			delegate = createDelegate();
		return delegate;
	}
	
	protected WSServiceDelegate createDelegate() {
		return new WSServiceDelegate(null, (WSDLServiceImpl) (seiModel != null ? seiModel.getPort()
				.getOwner() : null), serviceName, Service.class);
	}

    /**
     * Get a Dispatch instance suitable for sending new requests to a target
     * endpoint that is 'implied' by the current endpoint's service definition.
     * For example, if used to send RM protocol messages, the target endpoint
     * is implied to have the same policy as this endpoint.
     * @return
     */
    public Dispatch<Message>
    createMessageDispatch() {

      TubelineAssembler assembler = TubelineAssemblerFactory.create(
        Thread.currentThread().getContextClassLoader(), binding.getBindingId(),
        container);
      assert assembler!=null;

      EndpointAddress addr = null;
      try {
        addr = new EndpointAddress(binding.getAddressingVersion().anonymousUri);
      } catch (Exception e) {
        e.printStackTrace();
      }
      MyService service = new MyService();
      ClientTubeAssemblerContext context =
        new ClientTubeAssemblerContext(addr,
                                       port,
                                       service,
                                       binding,
                                       container);
      // TODO: Probably need to cache this as a 'master' tubeline and clone
      Tube tube = assembler.createClient(context);

      return new MyMessageDispatch(service.getExecutor(), tube);
    }

    /**
     * Skeletal WSService impl to satisfy the need for a WSService in creating
     * the Dispatch<Message> in getMessageDispatch. Without this, we NPE in some
     * places when the dispatch is used.
     */
    class MyService
      extends WSService {

      WSServiceDelegate _delegate;

      public MyService() {
        _delegate = getDelegate();
      }

      public <T> T getPort(WSEndpointReference epr, Class<T> portInterface,
                           WebServiceFeature... features) {
        throw new IllegalArgumentException("Not supported");
      }

      public <T> Dispatch<T> createDispatch(QName portName,
                                            WSEndpointReference wsepr,
                                            Class<T> aClass, javax.xml.ws.Service.Mode mode,
                                            WebServiceFeature... features) {
        throw new IllegalArgumentException("Not supported");
      }

      public Dispatch<Object> createDispatch(QName portName,
                                             WSEndpointReference wsepr,
                                             JAXBContext jaxbContext,
                                             javax.xml.ws.Service.Mode mode,
                                             WebServiceFeature... features) {
        throw new IllegalArgumentException("Not supported");
      }

      public
      @NotNull
      Container getContainer() {
        return container;
      }

      public <T> T getPort(QName portName, Class<T> serviceEndpointInterface) {
        throw new IllegalArgumentException("Not supported");
      }

      public <T> T getPort(QName portName, Class<T> serviceEndpointInterface,
                           WebServiceFeature... features) {
        throw new IllegalArgumentException("Not supported");
      }

      public <T> T getPort(EndpointReference endpointReference,
                           Class<T> serviceEndpointInterface,
                           WebServiceFeature... features) {
        throw new IllegalArgumentException("Not supported");
      }

      public <T> T getPort(Class<T> serviceEndpointInterface) {
        throw new IllegalArgumentException("Not supported");
      }

      public <T> T getPort(Class<T> serviceEndpointInterface,
                           WebServiceFeature... features) {
        throw new IllegalArgumentException("Not supported");
      }

      public void addPort(QName portName, String bindingId,
                          String endpointAddress) {
        throw new IllegalArgumentException("Not supported");
      }

      public <T> Dispatch<T> createDispatch(QName portName, Class<T> type,
                                            javax.xml.ws.Service.Mode mode) {
        throw new IllegalArgumentException("Not supported");
      }

      public <T> Dispatch<T> createDispatch(QName portName, Class<T> type,
                                            javax.xml.ws.Service.Mode mode,
                                            WebServiceFeature... features) {
        throw new IllegalArgumentException("Not supported");
      }

      public <T> Dispatch<T> createDispatch(EndpointReference endpointReference,
                                            Class<T> type, javax.xml.ws.Service.Mode mode,
                                            WebServiceFeature... features) {
        throw new IllegalArgumentException("Not supported");
      }

      public Dispatch<Object> createDispatch(QName portName, JAXBContext context,
                                             javax.xml.ws.Service.Mode mode) {
        throw new IllegalArgumentException("Not supported");
      }

      public Dispatch<Object> createDispatch(QName portName, JAXBContext context,
                                             javax.xml.ws.Service.Mode mode,
                                             WebServiceFeature... features) {
        throw new IllegalArgumentException("Not supported");
      }

      public Dispatch<Object> createDispatch(EndpointReference endpointReference,
                                             JAXBContext context, javax.xml.ws.Service.Mode mode,
                                             WebServiceFeature... features) {
        throw new IllegalArgumentException("Not supported");
      }

      public QName getServiceName() {
        return serviceName;
      }

      public Iterator<QName> getPorts() {
        throw new IllegalArgumentException("Not supported");
      }

      public URL getWSDLDocumentLocation() {
        return null;
      }

      public HandlerResolver getHandlerResolver() {
        throw new IllegalArgumentException("Not supported");
      }

      public void setHandlerResolver(HandlerResolver handlerResolver) {
        throw new IllegalArgumentException("Not supported");
      }

      public Executor getExecutor() {
        return _delegate.getExecutor();
      }

      public void setExecutor(Executor executor) {
        throw new IllegalArgumentException("Not supported");
      }
    }

    /**
     * Dispatch instance returned by getMessageDispatch. This dispatch can be
     * used to send new messages both sync and async.
     */
    class MyMessageDispatch
      implements Dispatch<Message>, Closeable {

      static final long AWAIT_TERMINATION_TIME = 800L;

      private Executor _exec;
      private Tube _tube;
      private Map<String, Object> _requestContext;

      public MyMessageDispatch(Executor exec, Tube tube) {
        _exec = exec;
        _tube = tube;
        _requestContext = new HashMap<String, Object>();
      }

      public Message invoke(Message msg) {

        Packet request = createPacket(msg);

        Fiber fiber = engine.createFiber();
        try {
          fiber.runSync(_tube, request);
        } catch(WebServiceException e){
          //it could be a WebServiceException or a ProtocolException
          throw e;
        } catch(Throwable e){
          // it could be a RuntimeException resulting due to some internal bug or
          // its some other exception resulting from user error, wrap it in
          // WebServiceException
          throw new WebServiceException(e);
        }

        Packet response = fiber.getPacket();
        return response != null ? response.getMessage() : null;
      }

      private Packet createPacket(Message msg) {
        Packet request = new Packet(msg);
        request.expectReply = true;
        String addrStr = (String)
          _requestContext.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        EndpointAddress addr;
        try {
          addr = new EndpointAddress(addrStr);
        } catch (Exception e) {
          throw new RuntimeException(e.toString(), e);
        }
        request.endpointAddress = addr;
        request.invocationProperties.putAll(_requestContext);
        // SOAP Action
        if (request.soapAction == null) {
          String soapAction = (String)
            _requestContext.get(BindingProvider.SOAPACTION_URI_PROPERTY);
          request.soapAction = soapAction;
        }
        return request;
      }

      public Response<Message> invokeAsync(Message msg) {
        AsyncInvoker invoker = new DispatchAsyncInvoker(msg);
        AsyncResponseImpl<Message> ft = new AsyncResponseImpl<Message>(invoker,null);
        invoker.setReceiver(ft);
        _exec.execute(ft);
        return ft;
      }

      public Future<?> invokeAsync(Message param,
                                   AsyncHandler<Message> asyncHandler) {
        AsyncInvoker invoker = new DispatchAsyncInvoker(param);
        invoker.setNonNullAsyncHandlerGiven(asyncHandler != null);
        AsyncResponseImpl<Message> ft =
          new AsyncResponseImpl<Message>(invoker,asyncHandler);
        invoker.setReceiver(ft);

        // temp needed so that unit tests run and complete otherwise they may
        //not. Need a way to put this in the test harness or other way to do this
        //todo: as above
        Executor ex = _exec;
        if (ex instanceof ExecutorService) {
	        ExecutorService exec = (ExecutorService) ex;
	        try {
	            exec.awaitTermination(AWAIT_TERMINATION_TIME, TimeUnit.MICROSECONDS);
	        } catch (InterruptedException e) {
	            throw new WebServiceException(e);
	        }
        }
        ex.execute(ft);
        return ft;
      }

      public void invokeOneWay(Message msg) {
        invokeAsync(msg);
      }

      public Map<String, Object> getRequestContext() {
        return _requestContext;
      }

      public Map<String, Object> getResponseContext() {
        return _requestContext;
      }

      public Binding getBinding() {
        return binding;
      }

      public EndpointReference getEndpointReference() {
        return null;
      }

      public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
        return null;
      }

      public void close() throws IOException {
        if (_tube != null) {
          // multi-thread safety of 'close' needs to be considered more carefully.
          // some calls might be pending while this method is invoked. Should we
          // block until they are complete, or should we abort them (but how?)
          Tube tube = _tube;
          _tube = null;
          tube.preDestroy();
        }
      }

      /**
       * AsyncInvoker impl that takes our stored Message, creates a Packet
       * to hold it, and starts it down a Fiber asynchronously, setting up
       * callback stuff to get the results.
       */
      private class DispatchAsyncInvoker extends AsyncInvoker {
          private final Message param;

          DispatchAsyncInvoker(Message param) {
              this.param = param;
          }

          public void do_run () {

              Packet packet = createPacket(param);
              packet.nonNullAsyncHandlerGiven = this.nonNullAsyncHandlerGiven;
              packet.expectReply = false; // async generally won't get response
              String action = null;
              String msgId = null;
              try {
                if (LOGGER.isLoggable(Level.FINE)) {
                  AddressingVersion av = WSEndpointImpl.this.getBinding().getAddressingVersion();
                  SOAPVersion sv = WSEndpointImpl.this.getBinding().getSOAPVersion();
                  action =
                    av != null ?
                      packet.getMessage().getHeaders().getAction(av, sv) : null;
                  msgId =
                    av != null ?
                      packet.getMessage().getHeaders().getMessageID(av, sv) : null;
                  LOGGER.fine("In AsyncInvoker.do_run for async message with action: " + action + " and msg ID: " + msgId);
                }
                Packet response = engine.createFiber().runSync(_tube, packet);
                if (LOGGER.isLoggable(Level.FINE)) {
                  LOGGER.fine("Done with invokeSync in AsyncInvoker.do_run, and setting response for async message with action: " + action + " and msg ID: " + msgId + " response msg: " + response.getMessage());
                }
                responseImpl.set(response.getMessage(), null);
              } catch (Throwable error) {
                if (LOGGER.isLoggable(Level.FINE)) {
                  LOGGER.fine("Done with invokeSync in AsyncInvoker.do_run, and setting response for async message with action: " + action + " and msg ID: " + msgId + " response Throwable: " + error.toString());
                }
                responseImpl.set(null, error);
                }
              }
          }
      }
    }

