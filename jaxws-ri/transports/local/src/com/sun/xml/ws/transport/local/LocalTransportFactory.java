package com.sun.xml.ws.transport.local;

import com.sun.xml.ws.api.pipe.TransportPipeFactory;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.transport.http.DeploymentDescriptorParser;
import com.sun.xml.ws.transport.http.DeploymentDescriptorParser.AdapterFactory;

import java.net.URI;
import java.util.List;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Map;

/**
 * {@link TransportPipeFactory} for the local transport.
 *
 * @author Kohsuke Kawaguchi
 */
public final class LocalTransportFactory extends TransportPipeFactory {
    public Pipe doCreate(EndpointAddress addres, WSDLPort wsdlModel, WSService service, WSBinding binding) {
        URI adrs = addres.getURI();
        if(!adrs.getScheme().equals("local"))
            return null;

        return new LocalTransportPipe(createServerService(wsdlModel),binding);
    }

    /**
     * The local transport works by 'deploying' a service , we "deploy"
     */
    protected static WSEndpoint createServerService(WSDLPort wsdlModel) {
        try {
            String outputDir = System.getProperty("tempdir");
            if (outputDir == null) {
                throw new Error("Set tempdir system property");
            }
            List<WSEndpoint> endpoints = parseEndpoints(outputDir);

            WSEndpoint endpoint = endpoints.get(0);
            if (endpoints.size() > 1) {
                for (WSEndpoint rei : endpoints) {
                    if(rei.getPort().getName().equals(wsdlModel.getName())) {
                        endpoint = rei;
                        break;
                    }
                }
            }

            return endpoint;
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    protected static List<WSEndpoint> parseEndpoints(String outputDir) throws IOException {
        String riFile = outputDir+"/WEB-INF/sun-jaxws.xml";
        DeploymentDescriptorParser<WSEndpoint> parser = new DeploymentDescriptorParser<WSEndpoint>(
            Thread.currentThread().getContextClassLoader(),
            new FileSystemResourceLoader(new File(outputDir)), null,
            new AdapterFactory<WSEndpoint>() {
                public WSEndpoint createAdapter(String name, String urlPattern, WSEndpoint<?> endpoint, Class implementorClass) {
                    return endpoint;
                }
            });
        InputStream in = new FileInputStream(riFile);
        List<WSEndpoint> endpoints = parser.parse(in);
        in.close();

        return endpoints;
    }
}
