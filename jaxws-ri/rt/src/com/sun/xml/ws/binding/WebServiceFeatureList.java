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
package com.sun.xml.ws.binding;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.WSFeatureList;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.developer.MemberSubmissionAddressing;
import com.sun.xml.ws.developer.MemberSubmissionAddressingFeature;
import com.sun.xml.ws.developer.Stateful;
import com.sun.xml.ws.developer.StatefulFeature;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;

import javax.xml.ws.RespectBinding;
import javax.xml.ws.RespectBindingFeature;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.MTOM;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.spi.WebServiceFeatureAnnotation;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Represents a list of {@link WebServiceFeature}s that has bunch of utility methods
 * pertaining to web service features.
 *
 * @author Rama Pulavarthi
 */
public final class WebServiceFeatureList implements WSFeatureList {
    private  Map<Class<? extends WebServiceFeature>, WebServiceFeature> wsfeatures =
            new HashMap<Class<? extends WebServiceFeature>, WebServiceFeature>();

    public WebServiceFeatureList() {
    }

    public WebServiceFeatureList(@NotNull WebServiceFeature... features) {
        if(features != null)
            for(WebServiceFeature f: features) {
                wsfeatures.put(f.getClass(),f);
            }
    }

    /**
     * Creates a list by reading featuers from the annotation on a class.
     */
    public WebServiceFeatureList(@NotNull Class<?> endpointClass) {
        parseAnnotations(endpointClass);
    }

    /**
     * Reads {@link WebServiceFeatureAnnotation feature annotations} on a class
     * and adds them to the list.
     */
    public void parseAnnotations(Class<?> endpointClass) {
        for (Annotation a : endpointClass.getAnnotations()) {
            // TODO: this really needs generalization
            WebServiceFeature ftr;
            if (!(a.annotationType().isAnnotationPresent(WebServiceFeatureAnnotation.class))) {
                continue;
            } else if (a instanceof Addressing) {
                Addressing addAnn = (Addressing) a;
                ftr = new AddressingFeature(addAnn.enabled(), addAnn.required());
            } else if (a instanceof MemberSubmissionAddressing) {
                MemberSubmissionAddressing addAnn = (MemberSubmissionAddressing) a;
                ftr = new MemberSubmissionAddressingFeature(addAnn.enabled(), addAnn.required());
            } else if (a instanceof MTOM) {
                MTOM mtomAnn = (MTOM) a;
                ftr = new MTOMFeature(mtomAnn.enabled(), mtomAnn.threshold());
            } else if (a instanceof RespectBinding) {
                RespectBinding rbAnn = (RespectBinding) a;
                ftr = new RespectBindingFeature(rbAnn.enabled());
            } else if (a instanceof Stateful) {
                ftr = new StatefulFeature();
            } else {
                throw new WebServiceException("Unrecognized annotation:" + a);
            }
            addFeature(ftr);
        }
    }

    public Iterator<WebServiceFeature> iterator() {
        return wsfeatures.values().iterator();
    }

    public @NotNull WebServiceFeature[] toArray() {
        return wsfeatures.values().toArray(new WebServiceFeature[]{});
    }
    
    public boolean isFeatureEnabled(String featureId) {
        WebServiceFeature ftr = getFeature(featureId);
        if(ftr == null) {
            return false;
        }
        return ftr.isEnabled();
    }

    public boolean isFeatureEnabled(@NotNull Class<? extends WebServiceFeature> feature){
        WebServiceFeature ftr = getFeature(feature);
        if(ftr == null) {
            return false;
        }
        return ftr.isEnabled();
    }

    public @Nullable WebServiceFeature getFeature(String featureId) {
        if (featureId == null)
            return null;
        for(WebServiceFeature f: wsfeatures.values()){
            if(f.getID().equals(featureId))
                return f;
        }
        return null;
    }

    public @Nullable <F extends WebServiceFeature> F getFeature(@NotNull Class<F> featureType){
        return featureType.cast(wsfeatures.get(featureType));
    }

    /**
     * Adds a feature to the list if it's not already added.
     */
    public void addFeature(@NotNull WebServiceFeature f) {
        if(!wsfeatures.containsKey(f.getClass()))
            wsfeatures.put(f.getClass(), f);
    }

    /**
     * Adds features to the list if it's not already added.
     */
    public void addFeatures(@NotNull WSFeatureList list) {
        for (WebServiceFeature f : list)
            addFeature(f);
    }

    /**
     * Extracts features from {@link WSDLPortImpl#getFeatures()}.
     *
     * @param wsdlPort WSDLPort model
     * @param honorWsdlRequired : If this is true add WSDL Feature only if wsd:Required=true
     *          In SEI case, it should be false
     *          In Provider case, it should be true
     * @return Extra features that are not already set on binding.
     *         i.e, if a feature is set already on binding through someother API
     *         the coresponding wsdlFeature is not set.
     */
    public void mergeFeatures(@NotNull WSDLPort wsdlPort, boolean honorWsdlRequired) {
        if(honorWsdlRequired && !isFeatureEnabled(RespectBindingFeature.class))
            return;

        for (WebServiceFeature ftr : wsdlPort.getFeatures()) {
            //add this feature only if it not set already on binding.
            // as features from wsdl should not override features set through DD or annotations
            if (getFeature(ftr.getClass()) == null) {
                try {
                    // if is WSDL Extension , it will have required attribute
                    // Add only if isRequired returns true, when honorWsdlRequired is true
                    Method m = (ftr.getClass().getMethod("isRequired"));
                    try {
                        boolean required = (Boolean) m.invoke(ftr);
                        if (!honorWsdlRequired || required)
                            addFeature(ftr);
                    } catch (IllegalAccessException e) {
                        throw new WebServiceException(e);
                    } catch (InvocationTargetException e) {
                        throw new WebServiceException(e);
                    }
                } catch (NoSuchMethodException e) {
                    // this ftr is not an WSDL extension, just add it
                    addFeature(ftr);
                }
            }
        }
    }
}
