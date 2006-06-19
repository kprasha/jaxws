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

package com.sun.xml.ws.client.sei;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.ws.api.message.Headers;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.message.ByteArrayAttachment;
import com.sun.xml.ws.message.DataHandlerAttachment;
import com.sun.xml.ws.message.JAXBAttachment;
import com.sun.xml.ws.model.ParameterImpl;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;
import javax.activation.DataHandler;
import javax.xml.transform.Source;
import javax.xml.ws.WebServiceException;

/**
 * Puts a non-payload message parameter to {@link Message}.
 *
 * <p>
 * Instance of this class is used to handle header parameters and attachment parameters.
 * They add things to {@link Message}.
 *
 * @see BodyBuilder
 * @author Kohsuke Kawaguchi
 */
abstract class MessageFiller {

    /**
     * The index of the method invocation parameters that this object looks for.
     */
    protected final int methodPos;

    protected MessageFiller( int methodPos) {
        this.methodPos = methodPos;
    }

    /**
     * Moves an argument of a method invocation into a {@link Message}.
     */
    abstract void fillIn(Object[] methodArgs, Message msg);
    
    /**
     * Adds a parameter as an attachment.
     */
    static final class Attachment extends MessageFiller {
        private final ParameterImpl param;
        private final ValueGetter getter;
        private final String mimeType;

        protected Attachment(ParameterImpl param, ValueGetter getter) {
            super(param.getIndex());
            this.param = param;
            this.getter = getter;
            mimeType = param.getBinding().getMimeType();
        }

        void fillIn(Object[] methodArgs, Message msg) {
            String contentId;
            try {
                contentId = URLEncoder.encode(param.getPartName(), "UTF-8")+ '=' +UUID.randomUUID()+"@jaxws.sun.com";
            } catch (UnsupportedEncodingException e) {
                throw new WebServiceException(e);
            }
            
            Object obj = getter.get(methodArgs[methodPos]);
            com.sun.xml.ws.api.message.Attachment att = null;
            // TODO need to avoid the if for performance by creating custom
            // MessageFiller objects for each type
            if (obj instanceof DataHandler) {
                att = new DataHandlerAttachment(contentId,(DataHandler)obj);
            } else if(obj instanceof Source) {
                // this is potentially broken, as there's no guarantee this will work.
                // we should have our own AttachmentBlock implementation for this.
                att = new DataHandlerAttachment(contentId, new DataHandler(obj,mimeType));
            } else if (obj instanceof byte[]) {
                att = new ByteArrayAttachment(contentId,(byte[])obj,mimeType);
            } else if (isXMLMimeType(mimeType)) {
                att = new JAXBAttachment(contentId, obj, param.getBridge(), mimeType);
            } else {
                // this is also broken, as there's no guarantee that the object type and the MIME type
                // matches. But most of the time it matches, so it mostly works.
                att = new com.sun.xml.ws.message.DataHandlerAttachment(contentId,new DataHandler(obj,mimeType));
            }
            msg.getAttachments().add(att);
        }
    }

    /**
     * Adds a parameter as an header.
     */
    static final class Header extends MessageFiller {
        private final Bridge bridge;
        private final ValueGetter getter;

        protected Header(int methodPos, Bridge bridge, ValueGetter getter) {
            super(methodPos);
            this.bridge = bridge;
            this.getter = getter;
        }

        void fillIn(Object[] methodArgs, Message msg) {
            Object value = getter.get(methodArgs[methodPos]);
            msg.getHeaders().add(Headers.create(bridge,value));
        }
    }
    
    private static boolean isXMLMimeType(String mimeType){
        return (mimeType.equals("text/xml") || mimeType.equals("application/xml")) ? true : false;
    }

}
