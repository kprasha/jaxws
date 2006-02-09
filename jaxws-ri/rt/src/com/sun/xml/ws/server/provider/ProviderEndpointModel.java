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

package com.sun.xml.ws.server.provider;

import com.sun.xml.ws.api.message.Message;

import javax.xml.transform.Source;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;

/**
 * Keeps all the Provider endpoint information like Service.Mode etc. It also
 * does parameter binding(Provider has only one parameter, and one return value).
 * 
 * @author Jitendra Kotamraju
 */
abstract class ProviderEndpointModel {
        
    /**
     * Finds parameter type, mode and throws an exception if Service.Mode and
     * parameter type combination is invalid.
     */
    public abstract void createModel();
    
    /**
     * {@link Message} is converted to correct parameter for Provider.invoke() method
     */
    public abstract Object getParameter(Message msg);
    
    /**
     * return value of Provider.invoke() is converted to {@link Message}
     */
    public abstract Message getResponseMessage(Object returnValue);
    
    /*
     * Is it PAYLOAD or MESSAGE ??
     */
    protected static Service.Mode getServiceMode(Class<?> c) {
        ServiceMode mode = c.getAnnotation(ServiceMode.class);
        if (mode == null) {
            return Service.Mode.PAYLOAD;
        }
        return mode.value();
    }

    /*
     * Is it Provider<Source> ?
     */
    protected static boolean isSource(Class c) {
        try {
            c.getMethod("invoke",  Source.class);
            return true;
        } catch(NoSuchMethodException ne) {
            // ignoring intentionally
        }
        return false;
    }

}
