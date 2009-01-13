/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.ws.policy;

import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.policy.jaxws.spi.ModelConfiguratorProvider;
import com.sun.xml.ws.policy.privateutil.PolicyUtils;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.WebServiceException;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Rama Pulavarthi
 */
public class PolicyUtil {
    private static ModelConfiguratorProvider[] configurators = PolicyUtils.ServiceProvider.load(ModelConfiguratorProvider.class);

    public static void configureModel(final WSDLModel model, PolicyMap policyMap) throws PolicyException {
        for (ModelConfiguratorProvider configurator : configurators) {
                configurator.configure(model, policyMap);
            }
    }

    /**
     * TODO implement this by updating Policy SPI
     * @param policyMap
     * @param serviceName
     * @param portName
     * @return
     */
    public static List<WebServiceFeature> getPortScopedFeatures(PolicyMap policyMap, QName serviceName, QName portName) {
       List<WebServiceFeature> features = new ArrayList<WebServiceFeature>();
       final PolicyMapKey key = PolicyMap.createWsdlEndpointScopeKey(serviceName,portName);
       for (ModelConfiguratorProvider configurator : configurators) {
           //TODO create policy SPI
             //WebServiceFeature f = configurator.getFeature(key, policyMap);
            //features.add(f);
       }
       return features; 
    }

    /**
     * Creates a PolicyMap with no entries.
     * @return
     */
    public static PolicyMap createEmptyPolicyMap(){
        PolicyMapBuilder builder = new PolicyMapBuilder();
        try {
            return builder.getPolicyMap();
        } catch (PolicyException e) {
            throw new WebServiceException(e);
        }
    }
}