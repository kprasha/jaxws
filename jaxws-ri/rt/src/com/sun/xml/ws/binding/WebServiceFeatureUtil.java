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
import java.util.List;
import java.util.ArrayList;
import java.lang.annotation.Annotation;

/**
 * @author Rama Pulavarthi
 */

public class WebServiceFeatureUtil {
    public static WebServiceFeature[] parseWebServiceFeatures(@NotNull Class<?> endpointClass) {
        List<WebServiceFeature> wsfeatures = null;
        Annotation[] anns = endpointClass.getAnnotations();
        if (anns == null)
            return null;
        wsfeatures = new ArrayList<WebServiceFeature>();
        for (Annotation a : anns) {
            WebServiceFeature ftr = null;
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
                //TODO throw Exception
            }
            wsfeatures.add(ftr);
        }
        return wsfeatures.toArray(new WebServiceFeature[]{});
    }

    public static boolean isFeatureEnabled(@NotNull String featureID, WebServiceFeature[] wsfeatures) {
            if(wsfeatures == null)
                return false;
            for(WebServiceFeature ftr:wsfeatures) {
                if(ftr.getID().equals(featureID) && ftr.isEnabled()) {
                    return true;
                }
            }
            return false;
        }

        public static WebServiceFeature getFeature(@NotNull String featureID, WebServiceFeature[] wsfeatures) {
            if(wsfeatures == null)
                return null;
            for(WebServiceFeature ftr:wsfeatures) {
                if(ftr.getID().equals(featureID)) {
                    return ftr;
                }
            }
            return null;
        }

}
