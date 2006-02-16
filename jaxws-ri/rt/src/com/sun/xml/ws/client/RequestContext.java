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

import com.sun.xml.ws.util.PropertySet;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.message.Packet;

import javax.xml.ws.BindingProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Map.Entry;

/**
 * Request context implementation.
 *
 * <h2>Why a custom map?</h2>
 * <p>
 * The JAX-WS spec exposes properties as a {@link Map}, but if we just use
 * an ordinary {@link HashMap} for this, it doesn't work as fast as we'd like
 * it to be. Hence we have this class.
 *
 * <p>
 * We expect the user to set a few properties and then use that same
 * setting to make a bunch of invocations. So we'd like to take some hit
 * when the user actually sets a property to do some computation,
 * then use that computed value during a method invocation again and again.
 *
 * <p>
 * For this goal, we use {@link PropertySet} and implement some properties
 * as virtual properties backed by methods. This allows us to do the computation
 * in the setter, and store it in a field.
 *
 * <p>
 * These fields are used by {@link Stub#process} to populate a {@link Packet}.
 *
 *
 *
 * <h2>How it works?</h2>
 * <p>
 * We make an assumption that a request context is mostly used to just
 * get and put values, not really for things like enumerating or size.
 *
 * <p>
 * So we start by maintaining state as a combination of {@link #others}
 * bag and strongly-typed fields. As long as the application uses
 * just {@link Map#put}, {@link Map#get}, and {@link Map#putAll}, we can
 * do things in this way. In this mode a {@link Map} we return works as
 * a view into {@link RequestContext}, and by itself it maintains no state.
 *
 * <p>
 * If {@link RequestContext} is in this mode, its state can be copied
 * efficiently into {@link Packet}.
 *
 * <p>
 * Once the application uses any other {@link Map} method, we move to
 * the "fallback" mode, where the data is actually stored in a {@link HashMap},
 * this is necessary for implementing the map interface contract correctly.
 *
 * <p>
 * To be safe, once we fallback, we'll never come back to the efficient state.
 *
 *
 *
 * <h2>Caution</h2>
 * <p>
 * Once we are in the fallback mode, none of the strongly typed field will
 * be used, and they may contain stale values. So the only method
 * the code outside this class can safely use is {@link #copy()},
 * {@link #fill(Packet)}, and constructors. Do not access the strongly
 * typed fields nor {@link #others} directly.
 *
 * @author Kohsuke Kawaguchi
 */
@SuppressWarnings({"SuspiciousMethodCalls"})
public final class RequestContext extends PropertySet {
    /**
     * Stores properties that don't fit the strongly-typed fields.
     */
    private final Map<String,Object> others;

    /**
     * The endpoint address to which this message is sent to.
     *
     * <p>
     * This is the actual data store for {@link BindingProvider#ENDPOINT_ADDRESS_PROPERTY}.
     */
    private EndpointAddress endpointAddress;

    /**
     * Creates {@link BindingProvider#ENDPOINT_ADDRESS_PROPERTY} view
     * on top of {@link #endpointAddress}.
     *
     * @deprecated
     *      always access {@link #endpointAddress}.
     */
    @Property(BindingProvider.ENDPOINT_ADDRESS_PROPERTY)
    public String getEndPointAddressString() {
        if(endpointAddress==null)
            return null;
        else
            return endpointAddress.toString();
    }

    public void setEndPointAddressString(String s) {
        if(s==null)
            this.endpointAddress = null;
        else
            this.endpointAddress = EndpointAddress.create(s);
    }

    /**
     * {@link Map} exposed to the user application.
     */
    private final MapView mapView = new MapView();

    /**
     * Creates an empty {@link RequestContext}.
     */
    public RequestContext() {
        others = new HashMap<String, Object>();
    }

    /**
     * Copy constructor.
     */
    private RequestContext(RequestContext that) {
        others = new HashMap<String,Object>(that.others);
        // this is fragile, but it works faster
    }

    /**
     * The efficient get method that reads from {@link RequestContext}.
     */
    public Object get(Object key) {
        if(super.supports(key))
            return super.get(key);
        else
            return others.get(key);
    }

    /**
     * The efficient put method that updates {@link RequestContext}.
     */
    public Object put(String key, Object value) {
        if(super.supports(key))
            return super.put(key,value);
        else
            return others.put(key,value);
    }

    /**
     * Gets the {@link Map} view of this request context.
     *
     * @return
     *      Always same object. Returned map is live.
     */
    public Map<String,Object> getMapView() {
        return mapView;
    }

    /**
     * Fill a {@link Packet} with values of this {@link RequestContext}.
     */
    public void fill(Packet packet) {
        if(mapView.fallbackMap==null) {
            if(endpointAddress!=null)
                packet.endpointAddress = endpointAddress;
            packet.invocationProperties.putAll(others);
        } else {
            // fallback mode, simply copy map in a slow way
            for (Entry<String,Object> entry : mapView.fallbackMap.entrySet()) {
                String key = entry.getKey();
                if(packet.supports(key))
                    packet.put(key,entry.getValue());
                else
                    packet.invocationProperties.put(key,entry.getValue());
            }
        }
    }

    public RequestContext copy() {
        return new RequestContext(this);
    }


    private final class MapView implements Map<String,Object> {
        private Map<String,Object> fallbackMap;

        private Map<String,Object> fallback() {
            if(fallbackMap==null) {
                // has to fall back. fill in fallbackMap
                fallbackMap = new HashMap<String,Object>(others);
                // then put all known properties
                for (Map.Entry<String,Accessor> prop : propMap.entrySet()) {
                    fallbackMap.put(prop.getKey(),prop.getValue().get(RequestContext.this));
                }
            }
            return fallbackMap;
        }

        public int size() {
            return fallback().size();
        }

        public boolean isEmpty() {
            return fallback().isEmpty();
        }

        public boolean containsKey(Object key) {
            return fallback().containsKey(key);
        }

        public boolean containsValue(Object value) {
            return fallback().containsValue(value);
        }

        public Object get(Object key) {
            if (fallbackMap ==null) {
                return RequestContext.this.get(key);
            } else {
                return fallback().get(key);
            }
        }

        public Object put(String key, Object value) {
            if(fallbackMap ==null)
                return RequestContext.this.put(key,value);
            else
                return fallback().put(key, value);
        }

        public Object remove(Object key) {
            if (fallbackMap ==null) {
                return RequestContext.this.remove(key);
            } else {
                return fallback().remove(key);
            }
        }

        public void putAll(Map<? extends String, ? extends Object> t) {
            for (Entry<? extends String, ? extends Object> e : t.entrySet()) {
                put(e.getKey(),e.getValue());
            }
        }

        public void clear() {
            fallback().clear();
        }

        public Set<String> keySet() {
            return fallback().keySet();
        }

        public Collection<Object> values() {
            return fallback().values();
        }

        public Set<Entry<String, Object>> entrySet() {
            return fallback().entrySet();
        }
    }

    protected PropertyMap getPropertyMap() {
        return propMap;
    }

    private static final PropertyMap propMap = parse(RequestContext.class);
}
