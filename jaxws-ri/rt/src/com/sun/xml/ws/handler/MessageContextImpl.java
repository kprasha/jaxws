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

package com.sun.xml.ws.handler;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.util.ReadOnlyPropertyException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;

/**
 *
 * @author WS Development Team
 */

public class MessageContextImpl implements MessageContext {
    
    Map<String,Object> internalMap = new HashMap<String,Object>();
    Set<String> appScopeProps;
    /** Creates a new instance of MessageContextImpl */
    public MessageContextImpl(Packet packet) {
        
        internalMap.putAll(packet.createMapView());
        internalMap.putAll(packet.invocationProperties);
        internalMap.putAll(packet.otherProperties);
        appScopeProps =  packet.getApplicationScopePropertyNames(false);
        
    }
    
    protected void updatePacket() {
        throw new UnsupportedOperationException("wrong call");
    }
    public void setScope(String name, Scope scope) {
        //TODO: check in intrenalMap
        if(scope == MessageContext.Scope.APPLICATION) {
            appScopeProps.add(name);
        } else {
            appScopeProps.remove(name);
            
        }
    }
    
    public Scope getScope(String name) {
        if(appScopeProps.contains(name)) {
            return MessageContext.Scope.APPLICATION;
        } else {
            return MessageContext.Scope.HANDLER;
        }
    }
    
    public int size() {
        return internalMap.size();
    }
    
    public boolean isEmpty() {
        return internalMap.isEmpty();
    }
    
    public boolean containsKey(Object key) {
        return internalMap.containsKey(key);
    }
    
    public boolean containsValue(Object value) {
        return internalMap.containsValue(value);
    }
    
    public Object put(String key, Object value) {
        return internalMap.put(key,value);
    }
    public Object get(Object key) {
        return internalMap.get(key);
    }
    
    public void putAll(Map<? extends String, ? extends Object> t) {
        internalMap.putAll(t);
    }
    
    public void clear() {
        internalMap.clear();
    }
    public Object remove(Object key){
        return internalMap.remove(key);
    }
    public Set<String> keySet() {
        return internalMap.keySet();
    }
    public Set<Map.Entry<String, Object>> entrySet(){
        return internalMap.entrySet();
    }
    public Collection<Object> values() {
        return internalMap.values();
    }
    
    /**
     * Fill a {@link Packet} with values of this {@link MessageContext}.
     */
    protected void fill(Packet packet) {
        for (Entry<String,Object> entry : internalMap.entrySet()) {
                String key = entry.getKey();
                if(packet.supports(key)) {
                    try {
                    packet.put(key,entry.getValue());
                    } catch(ReadOnlyPropertyException e) {
                        // Nothing to do
                    }
                } else if(packet.otherProperties.containsKey(key)) {
                    packet.otherProperties.put(key,entry.getValue());
                } else {
                    packet.invocationProperties.put(key,entry.getValue());
                }
        }
        
        //Remove properties which are removed by user.
        packet.createMapView().keySet().retainAll(internalMap.keySet());
        packet.otherProperties.keySet().retainAll(internalMap.keySet());
        packet.invocationProperties.keySet().retainAll(internalMap.keySet());
        packet.getApplicationScopePropertyNames(false).retainAll(internalMap.keySet());
    }
    
}