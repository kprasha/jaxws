/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
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
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.WSFeatureList;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundPortType;
import com.sun.xml.ws.api.model.wsdl.WSDLService;
import com.sun.xml.ws.api.pipe.Codec;
import com.sun.xml.ws.api.pipe.Engine;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.api.pipe.FiberContextSwitchInterceptor;
import com.sun.xml.ws.api.pipe.ServerPipeAssemblerContext;
import com.sun.xml.ws.api.pipe.ServerTubeAssemblerContext;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.TubeCloner;
import com.sun.xml.ws.api.pipe.TubelineAssembler;
import com.sun.xml.ws.api.pipe.TubelineAssemblerFactory;
import com.sun.xml.ws.api.server.*;
import com.sun.xml.ws.fault.SOAPFaultBuilder;
import com.sun.xml.ws.model.wsdl.WSDLProperties;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;
import com.sun.xml.ws.resources.HandlerMessages;
import com.sun.xml.ws.util.RuntimeVersion;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.InheritedAttribute;
import org.glassfish.gmbal.InheritedAttributes;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedData;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.gmbal.ManagedObjectManager;
import org.glassfish.gmbal.ManagedObjectManagerFactory;
import java.net.URL;

import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.stream.XMLStreamException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Harold Carr
 */
@ManagedObject
@Description("Metro Web Service endpoint")
@AMXMetadata(type="Service")
public final class MonitorRootService {

    private final WSEndpoint endpoint;

    MonitorRootService(final WSEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.endpoint");

    //
    // Items from WSEndpoint
    //

    @ManagedAttribute
    @Description("Policy associated with Endpoint")
    public String policy() {
        return endpoint.getPolicyMap() != null ?
               endpoint.getPolicyMap().toString() : null;
    }

    @ManagedAttribute
    @Description("Container")
    public @NotNull Container container() {
        return endpoint.getContainer();
    }


    @ManagedAttribute
    @Description("Port name")
    public @NotNull QName portName() {
        return endpoint.getPortName();
    }

    @ManagedAttribute
    @Description("Service name")
    public @NotNull QName serviceName() {
        return endpoint.getServiceName();
    }

    //
    // Items from assembler context
    //

    @ManagedAttribute
    @Description("The last tube in the dispatch chain")
    public @NotNull Tube terminalTube() {
        return endpoint.getAssemblerContext().getTerminalTube();
    }

    @ManagedAttribute
    @Description("True if tubeline is known to be used for serving synchronous transport")
    public boolean synchronous() {
        return endpoint.getAssemblerContext().isSynchronous();
    }

    @ManagedAttribute
    @Description("")
    public String codecMimeType() {
        return endpoint.getAssemblerContext().getCodec().getMimeType();
    }

    //
    // Items from WSBinding
    //

    @ManagedAttribute
    @Description("Binding SOAP Version")
    public String bindingSOAPVersionHttpBindingId() {
        return endpoint.getBinding().getSOAPVersion().httpBindingId;
    }

    @ManagedAttribute
    @Description("Binding Addressing Version")
    public AddressingVersion bindingAddressingVersion() {
        return endpoint.getBinding().getAddressingVersion();
    }

    @ManagedAttribute
    @Description("Binding Addressing Version")
    public @NotNull BindingID bindingID() {
        return endpoint.getBinding().getBindingId();
    }

    @ManagedAttribute
    @Description("Binding features")
    public @NotNull WSFeatureList bindingFeatures() {
        return endpoint.getBinding().getFeatures();
    }

    //
    // Items from WSDLPort
    //

    @ManagedAttribute
    @Description("WSDLPort name")
    public QName wsdlPortName() {
        return endpoint.getPort() != null ?
               endpoint.getPort().getName() : null; 
    }

    @ManagedAttribute
    @Description("WSDLPort bound port type")
    public WSDLBoundPortType wsdlBoundPortType() {
        return endpoint.getPort() != null ?
               endpoint.getPort().getBinding() : null;
    }

    @ManagedAttribute
    @Description("Endpoint address")
    public EndpointAddress wsdlEndpointAddress() {
        return endpoint.getPort() != null ?
               endpoint.getPort().getAddress() : null;
    }

    @ManagedAttribute
    @Description("WSDLPort owner")
    public WSDLService wsdlOwner() {
        return endpoint.getPort() != null ?
               endpoint.getPort().getOwner() : null;
    }

    //
    // Items from ServiceDefinition
    //

    @ManagedAttribute
    @Description("Service Definition root name")
    public QName serviceDefinitionRootName() {
        return endpoint.getServiceDefinition() != null ?
               endpoint.getServiceDefinition().getPrimary().getRootName() : null;
    }

    @ManagedAttribute
    @Description("True if this document is WSDL")
    public boolean serviceDefinitionIsWSDL() {
        return endpoint.getServiceDefinition() != null ?
               endpoint.getServiceDefinition().getPrimary().isWSDL() : null;
    }

    @ManagedAttribute
    @Description("True if this document is schema")
    public boolean serviceDefinitionIsSchema() {
        return endpoint.getServiceDefinition() != null ?
               endpoint.getServiceDefinition().getPrimary().isSchema() : null;
    }

    @ManagedAttribute
    @Description("Documents referenced")
    public Set<String> serviceDefinitionImports() {
        return endpoint.getServiceDefinition() != null ?
               endpoint.getServiceDefinition().getPrimary().getImports() : null;
    }

    @ManagedAttribute
    @Description("System ID where document is taken from")
    public URL serviceDefinitionURL() {
        return endpoint.getServiceDefinition() != null ?
               endpoint.getServiceDefinition().getPrimary().getURL() : null;
    }

    //
    // Items from SEIModel
    //

    @ManagedAttribute
    @Description("SEI model WSDL location")
    public String seiModelWSDLLocation() {
        return endpoint.getSEIModel() != null ? 
               endpoint.getSEIModel().getWSDLLocation() : null;
    }

    @ManagedAttribute
    @Description("SEI model service name")
    public QName seiModelServiceName() {
        return endpoint.getSEIModel() != null ? 
               endpoint.getSEIModel().getServiceQName() : null;
    }

    @ManagedAttribute
    @Description("SEI model port name")
    public QName seiModelPortName() {
        return endpoint.getSEIModel() != null ? 
               endpoint.getSEIModel().getPortName() : null;
    }

    @ManagedAttribute
    @Description("SEI model port type name")
    public QName seiModelPortTypeName() {
        return endpoint.getSEIModel() != null ? 
               endpoint.getSEIModel().getPortTypeName() : null;
    }

    @ManagedAttribute
    @Description("SEI model bound port type name")
    public QName seiModelBoundPortTypeName() {
        return endpoint.getSEIModel() != null ? 
               endpoint.getSEIModel().getBoundPortTypeName() : null;
    }

    @ManagedAttribute
    @Description("SEI model target namespace")
    public String seiModelTargetNamespace() {
        return endpoint.getSEIModel() != null ? 
               endpoint.getSEIModel().getTargetNamespace() : null;
    }

    //
    // Items from RuntimeVersion
    //

    @ManagedAttribute
    @Description("JAX-WS runtime version")
    public String jaxwsRuntimeVersion() {
        return RuntimeVersion.VERSION.toString();
    }

    //
    // Service-side ManagedObjectManager creation
    //
    @NotNull ManagedObjectManager createManagedObjectManager(final QName serviceName, final QName portName) {
        if (!monitoring) {
            return ManagedObjectManagerFactory.createNOOP();
        }
        try {
            // TBD: Decide final root name.
            // Most likely it will be "metro" when running inside GlassFish,
            // (under the AMX node), and "com.sun.metro" otherwise.
            final ManagedObjectManager managedObjectManager =
                ManagedObjectManagerFactory.createStandalone("metro");

            if (typelibDebug != -1) {
                managedObjectManager.setTypelibDebug(typelibDebug);
            }
            if (registrationDebug.equals("FINE")) {
                managedObjectManager.setRegistrationDebug(ManagedObjectManager.RegistrationDebugLevel.FINE);
            } else if (registrationDebug.equals("NORMAL")) {
                managedObjectManager.setRegistrationDebug(ManagedObjectManager.RegistrationDebugLevel.NORMAL);
            } else {
                managedObjectManager.setRegistrationDebug(ManagedObjectManager.RegistrationDebugLevel.NONE);
            }
            managedObjectManager.setRuntimeDebug(runtimeDebug);

            managedObjectManager.stripPrefix(
                "com.sun.xml.ws.server",
                "com.sun.xml.ws.rx.rm.runtime.sequence");

            // Add annotations to a standard class
            managedObjectManager.addAnnotation(javax.xml.ws.WebServiceFeature.class, DummyWebServiceFeature.class.getAnnotation(ManagedData.class));
            managedObjectManager.addAnnotation(javax.xml.ws.WebServiceFeature.class, DummyWebServiceFeature.class.getAnnotation(Description.class));
            managedObjectManager.addAnnotation(javax.xml.ws.WebServiceFeature.class, DummyWebServiceFeature.class.getAnnotation(InheritedAttributes.class));

            // Defer so we can register "this" as root from
            // within constructor.
            managedObjectManager.suspendJMXRegistration();

            // We include the service+portName to uniquely identify
            // the managed objects under it (since there can be multiple
            // services in the container).
            managedObjectManager.createRoot(
                this,
                serviceName.toString() + portName.toString() + "-"
                + String.valueOf(unique++)); // TBD: only append unique
                                             // if clash. Waiting for GMBAL RFE

            return managedObjectManager;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "TBD - Ignoring exception - starting up without monitoring", t);
            //throw new WebServiceException(t);
            return ManagedObjectManagerFactory.createNOOP();
        }
    }

    private static boolean monitoring        = true;
    private static int     typelibDebug      = -1;
    private static String  registrationDebug = "NONE";
    private static boolean runtimeDebug      = false;

    static {
        try {
            // You can control "endpoint" independently of "client" monitoring.
            String s = System.getProperty("com.sun.xml.ws.monitoring.endpoint");
            if (s != null && s.toLowerCase().equals("false")) {
                monitoring = false;
            }

            // You can turn off both "endpoint" and "client" monitoring
            // at once.
            s = System.getProperty("com.sun.xml.ws.monitoring");
            if (s != null && s.toLowerCase().equals("false")) {
                monitoring = false;
            }

            Integer i = Integer.getInteger("com.sun.xml.ws.monitoring.typelibDebug");
            if (i != null) {
                typelibDebug = i;
            }
            s = System.getProperty("com.sun.xml.ws.monitoring.registrationDebug");
            if (s != null) {
                registrationDebug = s.toUpperCase();
            }
            s = System.getProperty("com.sun.xml.ws.monitoring.runtimeDebug");
            if (s != null && s.toLowerCase().equals("true")) {
                runtimeDebug = true;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "TBD", e);
        }
    }

    // Necessary because same serviceName+portName can live in
    // different apps at different addresses.  We do not know the
    // address until the first request comes in, which is after
    // monitoring is setup.

    // TBD: Add address field to Service class below and fill it
    // in on first request.  That will make it easier for users.
    private static long unique = 1;

}

@ManagedData
@Description("WebServiceFeature")
@InheritedAttributes({
        @InheritedAttribute(methodName="getID", description="unique id for this feature"),
        @InheritedAttribute(methodName="isEnabled", description="true if this feature is enabled")
})
interface DummyWebServiceFeature {}