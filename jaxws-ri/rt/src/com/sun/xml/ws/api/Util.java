package com.sun.xml.ws.api;

import com.sun.istack.NotNull;
import com.sun.xml.ws.server.EndpointFactory;

import javax.xml.namespace.QName;

/**
 * Utility class to provide information that can be made available to the extensions during runtime.
 */
public final class Util {
    /**
     * Gives the wsdl:service default name computed from the endpoint implementaiton class
     */
    public static @NotNull QName getDefaultServiceName(Class endpointClass){
        return EndpointFactory.getDefaultServiceName(endpointClass);
    }

    /**
     * Gives the wsdl:service/wsdl:port default name computed from the endpoint implementaiton class
     */
    public static @NotNull QName getDefaultPortName(QName serviceName, Class endpointClass){
        return EndpointFactory.getDefaultPortName(serviceName, endpointClass);
    }
}
