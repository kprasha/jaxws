package com.sun.xml.ws.server.provider;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.server.InstanceResolver;
import javax.activation.DataSource;
import javax.xml.transform.Source;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;

/**
 * This pipe is used to invoke XML/HTTP {@link Provider} endpoints.
 *
 * @author Jitendra Kotamraju
 */
public class XMLProviderInvokerPipe extends ProviderInvokerPipe {
    private final boolean isSource;
    private final Service.Mode mode;
     
    public XMLProviderInvokerPipe(InstanceResolver<? extends Provider> instanceResolver, ProviderEndpointModel model) {
        super(instanceResolver);
        this.isSource = model.isSource();
        this.mode = model.getServiceMode();
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
        }  else {
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
    
    @Override
    public Message getResponseMessage(Exception e) {
        return null;
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
        }  else {
            if (isSource) {
                Source source = (Source)returnValue;
                // Current Message implementation think that it is a SOAP Message
                responseMsg = Messages.createUsingPayload(source, SOAPVersion.SOAP_11);
            }  else {
                DataSource ds = (DataSource)returnValue;
                // TODO what message representation can we use here
                // None of the current implementations fit here
                responseMsg = null;
            }
        }
        return responseMsg;
    }
}