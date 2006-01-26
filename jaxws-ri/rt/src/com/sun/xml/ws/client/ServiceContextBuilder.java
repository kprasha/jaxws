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

import com.sun.xml.ws.util.JAXWSUtils;
import com.sun.xml.ws.wsdl.WSDLContext;
import org.xml.sax.EntityResolver;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * $author: WS Development Team
 */
public abstract class ServiceContextBuilder {
    private ServiceContextBuilder() {
    }  // no instantication please

    /**
     * Creates a new {@link ServiceContext}.
     */
    public static ServiceContext build(URL wsdlLocation, QName serviceName, Class service, EntityResolver er) throws WebServiceException {

        ServiceContext serviceContext = new ServiceContext(er);
        SCAnnotations serviceCAnnotations = null;

        if (service != javax.xml.ws.Service.class) {
            serviceCAnnotations = new SCAnnotations(service);
            serviceContext.setServiceClass(service);
            if(wsdlLocation!=null)
                wsdlLocation = serviceCAnnotations.wsdlLocation;
        }

        serviceContext.setWsdlContext(new WSDLContext(wsdlLocation, er));

        if (serviceCAnnotations != null) {
            for (Class clazz : serviceCAnnotations.classes) {
                serviceContext.addPort(clazz);
            }
        }

        return serviceContext;
    }

    public static void completeServiceContext(ServiceContext serviceContext, Class portInterface) {
        if ((serviceContext.getWsdlContext() == null) && (portInterface != null)) {
            URL wsdlLocation;
            try {
                wsdlLocation = new URL(JAXWSUtils.getFileOrURLName(getWSDLLocation(portInterface)));
            } catch (MalformedURLException e) {
                throw new WebServiceException(e);
            }

            serviceContext.setWsdlContext(new WSDLContext(wsdlLocation, serviceContext.getEntityResolver()));
        }

        if ((portInterface != null) && !serviceContext.hasSEI())
            serviceContext.addPort(portInterface);
    }


    //does any necessagy checking and validation


    /**
     * Utility method to get wsdlLocation attribute from @WebService annotation on sei.
     *
     * @return the URL of the location of the WSDL for the sei, or null if none was found.
     */
//this will change
    private static String getWSDLLocation(Class<?> sei) {
        WebService ws = sei.getAnnotation(WebService.class);
        if (ws == null)
            return null;
        return ws.wsdlLocation();
    }

//this will change

}
