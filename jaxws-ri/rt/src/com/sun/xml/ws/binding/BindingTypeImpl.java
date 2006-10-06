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
import com.sun.xml.ws.api.addressing.MemberSubmissionAddressingFeature;

import javax.xml.ws.*;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.MTOMFeature;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Rama Pulavarthi
 */

public class BindingTypeImpl {
    public static WebServiceFeature[] parseBindingType(@NotNull Class<?> endpointClass) {
            List<WebServiceFeature> wsfeatures = null;
            BindingType bindingType = endpointClass.getAnnotation(BindingType.class);
            if(bindingType != null) {
                Feature[] features = bindingType.features();
                if(features != null) {
                    wsfeatures = new ArrayList<WebServiceFeature>();
                    for(Feature f:features) {
                        if(f.enabled()) {
                            if(f.value().equals(AddressingFeature.ID) ) {
                                AddressingFeature addFeature = new AddressingFeature(true);
                                FeatureParameter[] params = f.parameters();
                                if(params != null) {
                                    for(FeatureParameter param: params) {
                                        if(param.name().equals(AddressingFeature.IS_REQUIRED)) {
                                            addFeature.setRequired(Boolean.valueOf(param.value()));
                                        }
                                    }
                                }
                                wsfeatures.add(addFeature);
                            } else if(f.value().equals(MemberSubmissionAddressingFeature.ID) ) {
                                MemberSubmissionAddressingFeature msaf = new MemberSubmissionAddressingFeature(true);
                                FeatureParameter[] params = f.parameters();
                                if(params != null) {
                                    for(FeatureParameter param: params) {
                                        if(param.name().equals(MemberSubmissionAddressingFeature.IS_REQUIRED)) {
                                            msaf.setRequired(Boolean.parseBoolean(param.value()));
                                        }
                                    }
                                }
                                wsfeatures.add(msaf);
                            } else if(f.value().equals(MTOMFeature.ID) ) {
                                MTOMFeature mtomfeature =new MTOMFeature(true);
                                FeatureParameter[] params = f.parameters();
                                if(params != null) {
                                    for(FeatureParameter param: params) {
                                        if(param.name().equals(MTOMFeature.THRESHOLD)) {
                                            mtomfeature.setThreshold(Integer.parseInt(param.value()));
                                        }
                                    }
                                }
                                wsfeatures.add(mtomfeature);
                            }
                        }
                    }
                }
            }
            return wsfeatures == null ? null : wsfeatures.toArray(new WebServiceFeature[] {});
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

}
