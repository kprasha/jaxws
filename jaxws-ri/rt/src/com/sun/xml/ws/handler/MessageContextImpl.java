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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import javax.xml.ws.handler.MessageContext;

/**
 *
 * @author WS Development Team
 */

class MessageContextImpl implements MessageContext {
    
    private Map<String,Object> internalMap = new HashMap<String,Object>();
    private Set<String> handlerScopeProps;
    Packet packet;
    boolean packetPropsAccessed = false;

    void populateMap() {
        if(!packetPropsAccessed) {
            packetPropsAccessed = true;
            handlerScopeProps =  packet.getHandlerScopePropertyNames(false);
            internalMap.putAll(packet.createMapView());
            internalMap.putAll(packet.invocationProperties);
        }
    }
    /** Creates a new instance of MessageContextImpl */
    public MessageContextImpl(Packet packet) {
        this.packet = packet;
    }
    protected void updatePacket() {
        throw new UnsupportedOperationException("wrong call");
    }
    public void setScope(String name, Scope scope) {
        populateMap();
        if (!keyExists(name)) throw new IllegalArgumentException("Property " + name + " does not exist.");
        
        //TODO: check in intrenalMap
        if(scope == Scope.APPLICATION) {
            handlerScopeProps.remove(name);
        } else {
            handlerScopeProps.add(name);
            
        }
    }
    
    public Scope getScope(String name) {
        populateMap();
        if (!keyExists(name)) throw new IllegalArgumentException("Property " + name + " does not exist.");

        if(handlerScopeProps.contains(name)) {
            return Scope.HANDLER;
        } else {
            return Scope.APPLICATION;
        }
    }
    
    public int size() {
        populateMap();
        return internalMap.size();
    }
    
    public boolean isEmpty() {
        populateMap();
        return internalMap.isEmpty();
    }
    
    public boolean containsKey(Object key) {
        populateMap();
        return internalMap.containsKey(key);
    }
    
    public boolean containsValue(Object value) {
        populateMap();
        return internalMap.containsValue(value);
    }
    
    public Object put(String key, Object value) {
        populateMap();
        if(!keyExists(key)) {
            //new property, default to Scope.HANDLER
            handlerScopeProps.add(key);
        }
        return internalMap.put(key,value);
    }
    public Object get(Object key) {
        populateMap();
        return internalMap.get(key);
    }
    
    public void putAll(Map<? extends String, ? extends Object> t) {
        populateMap();
        for(String key: t.keySet()) {
            if(!keyExists(key)) {
                //new property, default to Scope.HANDLER
                handlerScopeProps.add(key);
            }
        }
        internalMap.putAll(t);
    }
    
    public void clear() {
        populateMap();
        internalMap.clear();
    }
    public Object remove(Object key){
        populateMap();
        handlerScopeProps.remove(key);
        return internalMap.remove(key);
    }
    public Set<String> keySet() {
        populateMap();
        return internalMap.keySet();
    }
    public Set<Map.Entry<String, Object>> entrySet(){
        populateMap();
        return internalMap.entrySet();
    }
    public Collection<Object> values() {
        populateMap();
        return internalMap.values();
    }

    private boolean keyExists(String name){
        return keySet().contains(name);
    }
    
    /**
     * Fill a {@link Packet} with values of this {@link MessageContext}.
     */
    void fill(Packet packet) {
        if(packetPropsAccessed) {
            for (Entry<String, Object> entry : internalMap.entrySet()) {
                String key = entry.getKey();
                if (packet.supports(key)) {
                    try {
                        packet.put(key, entry.getValue());
                    } catch (ReadOnlyPropertyException e) {
                        // Nothing to do
                    }
                } else {
                    packet.invocationProperties.put(key, entry.getValue());
                }
            }

            //Remove properties which are removed by user.
            packet.createMapView().keySet().retainAll(internalMap.keySet());
            packet.invocationProperties.keySet().retainAll(internalMap.keySet());
        }
    }

}