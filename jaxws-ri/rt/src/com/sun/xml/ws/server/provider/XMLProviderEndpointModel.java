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

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;

import javax.activation.DataSource;
import javax.xml.transform.Source;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;

/**
 * Keeps all the {@link Provider} endpoint information for XML/HTTP binding. It
 * also does parameter binding(Provider endpoint has only one parameter, and one
 * return value).
 *
 * @author Jitendra Kotamraju
 */
final class XMLProviderEndpointModel extends ProviderEndpointModel {

    private Class implementorClass;
    private Service.Mode mode;
    private boolean isSource;

    public XMLProviderEndpointModel(Class implementorClass) {
        this.implementorClass = implementorClass;
    }

    /**
     * Finds parameter type, mode and throws an exception if Service.Mode and
     * parameter type combination is invalid.
     */
    @Override
    public void createModel() {
        isSource = isSource(implementorClass);
        boolean isDataSource = isDataSource(implementorClass);
        mode = getServiceMode(implementorClass);
        if (!(isSource || isDataSource)) {
            throw new IllegalArgumentException(
                "Endpoint should implement Provider<Source> or Provider<DataSource>");
        }
        if (mode == Service.Mode.PAYLOAD && isDataSource) {
            throw new IllegalArgumentException(
                "Illeagal combination Mode.PAYLOAD and Provider<DataSource>");
        }
    }

    /**
     * {@link Message} is converted to correct parameter for Provider.invoke() method
     */
    @Override
    public Object getParameter(Message msg) {
        Object parameter = null;
        if (mode == Service.Mode.PAYLOAD) {
            if (isSource) {
                parameter = msg.readPayloadAsSource();
            }
            // else doesn't happen because ProviderModel takes care of it
        } else {
            if (isSource) {
                parameter = msg.readPayloadAsSource();
            } else {
                // TODO : use Encoder to write attachments, and XML 
                // This is also performance issue. The transport bytes are
                // parsed and rewritten to make DataSource.
                /*
                parameter = new DataSource() {
                    public InputStream getInputStream() {
                        return is;
                    }

                    public OutputStream getOutputStream() {
                        return null;
                    }

                    public String getContentType() {
                        return contentType;
                    }

                    public String getName() {
                        return "";
                    }
                };
                 */
            }
        }
        return parameter;
    }

    /**
     * return value of Provider.invoke() is converted to {@link Message}
     */
    @Override
    public Message getResponseMessage(Object returnValue) {
        Message responseMsg;
        if (mode == Service.Mode.PAYLOAD) {
            Source source = (Source)returnValue;
            // Current Message implementation think that it is a SOAP Message
            responseMsg = Messages.createUsingPayload(source, SOAPVersion.SOAP_11);
        } else {
            if (isSource) {
                Source source = (Source)returnValue;
                // Current Message implementation think that it is a SOAP Message
                responseMsg = Messages.createUsingPayload(source, SOAPVersion.SOAP_11);
            } else {
                DataSource ds = (DataSource)returnValue;
                // TODO what message representation can we use here
                // None of the current implementations fit here
                responseMsg = null;
            }
        }
        return responseMsg;
    }

    /*
    * Is it Provider<DataSource> ?
    */
    private static boolean isDataSource(Class c) {
        try {
            c.getMethod("invoke",  DataSource.class);
            return true;
        } catch(NoSuchMethodException ne) {
            // ignoring intentionally
        }
        return false;
    }

}
