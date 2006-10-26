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
import com.sun.xml.ws.developer.MemberSubmissionAddressingFeature;
import com.sun.xml.ws.developer.MemberSubmissionAddressing;
import com.sun.xml.ws.developer.Stateful;
import com.sun.xml.ws.developer.StatefulFeature;

import javax.xml.ws.*;
import javax.xml.ws.spi.WebServiceFeatureAnnotation;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.MTOM;
import java.util.Map;
import java.util.HashMap;
import java.lang.annotation.Annotation;

/**
 * Represents a list of WebServiceFeatures that has bunch of utility methods
 * pertaining to web service features.
 *
 * @author Rama Pulavarthi
 */

public class WebServiceFeatureList {
    private  Map<Class<? extends WebServiceFeature>, WebServiceFeature> wsfeatures =
            new HashMap<Class<? extends WebServiceFeature>, WebServiceFeature>();

    public WebServiceFeatureList(@NotNull WebServiceFeature... features) {
        if(features != null)
            for(WebServiceFeature f: features) {
                wsfeatures.put(f.getClass(),f);
            }
    }
    public WebServiceFeatureList(@NotNull Class<?> endpointClass) {
        Annotation[] anns = endpointClass.getAnnotations();
        for (Annotation a : anns) {
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
            wsfeatures.put(ftr.getClass(),ftr);
        }
    }

    public @NotNull WebServiceFeature[] getFeatures() {
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

    <F extends WebServiceFeature> void addFeature(@NotNull F f) {
        wsfeatures.put(f.getClass(), f);
    }
}
