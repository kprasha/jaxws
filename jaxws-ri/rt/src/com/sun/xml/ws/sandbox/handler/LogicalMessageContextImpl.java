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
package com.sun.xml.ws.sandbox.handler;

import com.sun.xml.ws.api.message.AttachmentSet;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.MessageProperties;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.handler.*;
import com.sun.xml.ws.sandbox.handler.MessageContextImpl;
import com.sun.xml.ws.sandbox.message.impl.source.PayloadSourceMessage;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.ws.LogicalMessage;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.SOAPBinding;

/**
 * Implementation of LogicalMessageContext. This class is used at runtime
 * to pass to the handlers for processing logical messages.
 *
 * <p>This Class delegates most of the fuctionality to MessageProperties
 *
 * @see MessageProperties
 *
 * @author WS Development Team
 */
public class LogicalMessageContextImpl implements LogicalMessageContext {
    private Message msg;
    private MessageContext ctxt;
    private LogicalMessageImpl lm;
    private WSBinding binding;
    
    public LogicalMessageContextImpl(WSBinding binding, Message msg, MessageContext cxt) {
        this.binding = binding;
        this.msg = msg;
        this.ctxt = ctxt;
    }

    public LogicalMessage getMessage() {
        lm = new LogicalMessageImpl(msg);
        return lm;
    }
    
    protected Message getNewMessage() {
        //Check if LogicalMessageImpl has changed, if so construct new one
        //TODO: Attachments are not used
        // MessageProperties are handled through MessageContext 
        if(lm.payloadSrc != null){
            HeaderList headers = msg.getHeaders();
            AttachmentSet attchments = msg.getAttachments();
            msg = new PayloadSourceMessage(headers, lm.payloadSrc,binding.getSOAPVersion());            
        }
        return msg;
        
    }
    public void setScope(String name, Scope scope) {
        ctxt.setScope(name, scope);
    }

    public Scope getScope(String name) {
        return ctxt.getScope(name);
    }

    /* java.util.Map methods below here */
    
    public void clear() {
        ctxt.clear();
    }

    public boolean containsKey(Object obj) {
        return ctxt.containsKey(obj);
    }

    public boolean containsValue(Object obj) {
        return ctxt.containsValue(obj);
    }

    public Set<Entry<String, Object>> entrySet() {
        return ctxt.entrySet();
    }

    public Object get(Object obj) {
        return ctxt.get(obj);
    }

    public boolean isEmpty() {
        return ctxt.isEmpty();
    }

    public Set<String> keySet() {
        return ctxt.keySet();
    }

    public Object put(String str, Object obj) {
        return ctxt.put(str, obj);
    }

    public void putAll(Map<? extends String, ? extends Object> map) {
        ctxt.putAll(map);
    }

    public Object remove(Object obj) {
        return ctxt.remove(obj);
    }

    public int size() {
        return ctxt.size();
    }

    public Collection<Object> values() {
        return ctxt.values();
    }

}
