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
package com.sun.xml.ws.client;

import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.developer.MemberSubmissionAddressingFeature;
import com.sun.xml.ws.model.SOAPSEIModel;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.binding.SOAPBindingImpl;
import com.sun.istack.NotNull;

import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.MTOMFeature;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;


/**
 * {@link PortInfo} that has {@link SEIModel}.
 *
 * This object is created statically when {@link WSServiceDelegate} is created
 * with an service interface.
 *
 * @author Kohsuke Kawaguchi
 */
final class SEIPortInfo extends PortInfo {
    public final Class sei;
    /**
     * Model of {@link #sei}.
     */
    public final SOAPSEIModel model;

    public SEIPortInfo(WSServiceDelegate owner, Class sei, SOAPSEIModel model, @NotNull WSDLPort portModel) {
        super(owner,portModel);
        this.sei = sei;
        this.model = model;
        assert sei!=null && model!=null;
    }

    /**
     * This method inaddition to creating a BindingImpl object, sets the portKnownHeaders
     *  that can be used for MU Header processing.
     * @return BindingImpl
     */
    @Override
    public BindingImpl createBinding() {
        return createBinding(null);
    }

    public BindingImpl createBinding(WebServiceFeature[] webServiceFeatures) {
         BindingImpl bindingImpl = super.createBinding(resolveFeatures(webServiceFeatures));
         if(bindingImpl instanceof SOAPBindingImpl) {
            ((SOAPBindingImpl)bindingImpl).setPortKnownHeaders(model.getKnownHeaders());
         }
         //Not needed as set above in super.createBinding() call  
         //bindingImpl.setFeatures(webServiceFeatures);
         return bindingImpl;
    }

    protected List<WebServiceFeature> extractWSDLFeatures() {
        List<WebServiceFeature> wsdlFeatures = null;
        if (portModel != null) {
            wsdlFeatures = new ArrayList<WebServiceFeature>();
            WebServiceFeature wsdlAddressingFeature = portModel.getFeature(AddressingFeature.ID);
            if (wsdlAddressingFeature != null) {
                wsdlFeatures.add(wsdlAddressingFeature);
            } else {
                //try MS Addressing Version
                wsdlAddressingFeature = portModel.getFeature(MemberSubmissionAddressingFeature.ID);
                if (wsdlAddressingFeature != null)
                    wsdlFeatures.add(wsdlAddressingFeature);
            }

            WebServiceFeature wsdlMTOMFeature = portModel.getFeature(MTOMFeature.ID);
            if (wsdlMTOMFeature != null) {
                wsdlFeatures.add(wsdlMTOMFeature);
            }
            //these are the only features that jaxws pays attention portability wise.
        }
        return wsdlFeatures;
    }

    protected WebServiceFeature[] resolveFeatures(WebServiceFeature[] webServiceFeatures) {
        Map<String, WebServiceFeature> featureMap = fillMap(webServiceFeatures);
        List<WebServiceFeature> wsdlFeatures = extractWSDLFeatures();
        for(WebServiceFeature ftr: wsdlFeatures) {
            if(featureMap.get(ftr.getID()) == null) {
                featureMap.put(ftr.getID(),ftr);
            }
        }
        return featureMap.values().toArray(new WebServiceFeature[featureMap.size()]);
    }
/*
    protected void resolveAddressingFeature(List<WebServiceFeature> wsdlFeatures, Map<String, WebServiceFeature> featureMap) {
        AddressingFeature addressingFeature = (AddressingFeature) featureMap.get(AddressingFeature.ID);
        addressingFeature = (addressingFeature == null) ? (AddressingFeature) featureMap.get(MemberSubmissionAddressingFeature.ID): null;
        RespectBindingFeature bindingFeature = (RespectBindingFeature) featureMap.get(RespectBindingFeature.ID);
        if (addressingFeature != null) {
            if (!addressingFeature.isEnabled())  {  //let's just be explicit
                //if this is enabled just set it to required by spec rules
                addressingFeature.setRequired(false);
                bindingFeature = (bindingFeature == null)? new RespectBindingFeature(false): bindingFeature;
                featureMap.put(RespectBindingFeature.ID, bindingFeature);
            }
            //explicitly set RespectBindingFeature
        }
    }
    */
}
