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

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.addressing.MemberSubmissionAddressingFeature;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.binding.BindingTypeImpl;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;

import javax.xml.namespace.QName;
import javax.xml.ws.RespectBindingFeature;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.MTOMFeature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Information about a port.
 * <p/>
 * This object is owned by {@link WSServiceDelegate} to keep track of a port,
 * since a port maybe added dynamically.
 *
 * @author JAXWS Development Team
 */
public class PortInfo {
    private final
    @NotNull
    WSServiceDelegate owner;

    public final
    @NotNull
    QName portName;
    public final
    @NotNull
    EndpointAddress targetEndpoint;
    public final
    @NotNull
    BindingID bindingId;

    /**
     * If a port is known statically to a WSDL, {@link PortInfo} may
     * have the corresponding WSDL model. This would occur when the
     * service was created with the WSDL location and the port is defined
     * in the WSDL.
     * <p/>
     * If this is a {@link SEIPortInfo}, then this is always non-null.
     */
    public final
    @Nullable
    WSDLPort portModel;

    public PortInfo(WSServiceDelegate owner, EndpointAddress targetEndpoint, QName name, BindingID bindingId) {
        this.owner = owner;
        this.targetEndpoint = targetEndpoint;
        this.portName = name;
        this.bindingId = bindingId;
        this.portModel = getPortModel(owner, name);
    }

    public PortInfo(@NotNull WSServiceDelegate owner, @NotNull WSDLPort port) {
        this.owner = owner;
        this.targetEndpoint = port.getAddress();
        this.portName = port.getName();
        this.bindingId = port.getBinding().getBindingId();
        this.portModel = port;
    }

    public BindingImpl createBinding() {
        return owner.createBinding(portName, bindingId);
    }

    protected List<WebServiceFeature> extractWSDLFeatures() {
        List<WebServiceFeature> wsdlFeatures = null;
        if (portModel != null) {
            wsdlFeatures = new ArrayList<WebServiceFeature>();

            WebServiceFeature wsdlAddressingFeature = portModel.getFeature(AddressingFeature.ID);
            if (wsdlAddressingFeature != null) {
                //Set only if wsdl:required=true
                if (((AddressingFeature) wsdlAddressingFeature).isRequired())
                    wsdlFeatures.add(wsdlAddressingFeature);
            } else {
                //try MS Addressing Version
                wsdlAddressingFeature = portModel.getFeature(MemberSubmissionAddressingFeature.ID);
                //Set only if wsdl:required=true
                if (wsdlAddressingFeature != null &&
                        ((MemberSubmissionAddressingFeature) wsdlAddressingFeature).isRequired())
                    wsdlFeatures.add(wsdlAddressingFeature);
            }
            //Set only if wsdl:required=true
            if ((wsdlAddressingFeature != null) &&
                    ((AddressingFeature) wsdlAddressingFeature).isRequired()) {
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


    public BindingImpl createBinding(WebServiceFeature[] webServiceFeatures) {
        return owner.createBinding(portName, bindingId, resolveFeatures(webServiceFeatures));
    }

    private WSDLPort getPortModel(WSServiceDelegate owner, QName portName) {
        if (owner.getWsdlService() != null)
            return owner.getPortModel(portName);
        return null;
    }

    protected WebServiceFeature[] resolveFeatures(WebServiceFeature[] webServiceFeatures) {
        if (!BindingTypeImpl.isFeatureEnabled(RespectBindingFeature.ID, webServiceFeatures)) {
            return webServiceFeatures;
        }
        // RespectBindingFeature is enabled, so enable all wsdlFeatures
        Map<String, WebServiceFeature> featureMap = fillMap(webServiceFeatures);
        List<WebServiceFeature> wsdlFeatures = extractWSDLFeatures();
        //actually, the passed in WebServiceFeatures
        for (WebServiceFeature ftr : wsdlFeatures) {
            if (featureMap.get(ftr.getID()) == null) {
                featureMap.put(ftr.getID(), ftr);
            }
        }
        return featureMap.values().toArray(new WebServiceFeature[featureMap.size()]);
    }
/*
    protected WebServiceFeature[] resolveFeatures(WebServiceFeature[] webServiceFeatures) {

        List<WebServiceFeature> wsdlFeatures = extractWSDLFeatures();
        Map<String, WebServiceFeature> featureMap = fillMap(webServiceFeatures);

        resolveAddressingFeature(wsdlFeatures, featureMap);
        resolveMTOMFeature(featureMap);
        return featureMap.values().toArray(new WebServiceFeature[featureMap.size()]);
    }

    protected void resolveAddressingFeature(List<WebServiceFeature> wsdlFeatures, Map<String, WebServiceFeature> featureMap) {
        if (wsdlFeatures != null) {
            for (WebServiceFeature wsdlFeature : wsdlFeatures) {

                if (wsdlFeature.getID().equals(AddressingFeature.ID)) {
                    //look in webServiceFeatures
                    //look for RespectBindingFeature
                    RespectBindingFeature respectBindingFeature = (RespectBindingFeature) featureMap.get(RespectBindingFeature.ID);
                    // look for AddressingFeature
                    AddressingFeature addressingFeature = (AddressingFeature) featureMap.get(AddressingFeature.ID);
                    if (addressingFeature == null)
                        addressingFeature = (AddressingFeature) featureMap.get(MemberSubmissionAddressingFeature.ID);

                    if (addressingFeature == null) {
                        if (((AddressingFeature) wsdlFeature).isRequired() && (respectBindingFeature != null)) {
                            if (respectBindingFeature.isEnabled()) {
                                //add the AddressingFeature to
                                // the WebServiceFeatures
                                ((AddressingFeature) wsdlFeature).setRequired(true);
                                featureMap.put(wsdlFeature.getID(), wsdlFeature);
                            } else {
                                //explicitly disable addressing version
                                AddressingFeature disableAddressing = new AddressingFeature(false, false);
                                featureMap.put(disableAddressing.getID(), disableAddressing);
                                //respect Bind feature disables - for dispatch we do not respect binding
                            }
                        } //respectBindingFeature is null don't respect wsdl
                    }
                }
            }
        } else {
            //case without wsdl
            //lets just see if the AddressingFeature is enabled
            AddressingFeature addressingFeature = (AddressingFeature) featureMap.get(AddressingFeature.ID);

            if (addressingFeature == null)
                addressingFeature = (AddressingFeature) featureMap.get(MemberSubmissionAddressingFeature.ID);
            if (addressingFeature != null && addressingFeature.isEnabled()) {
                //if this is enabled just set it to required by spec rules
                addressingFeature.setRequired(true);
                //explicitly set this just in case someone looks for both
                RespectBindingFeature bindingFeature = (RespectBindingFeature) featureMap.get(RespectBindingFeature.ID);
                bindingFeature = (bindingFeature == null) ? new RespectBindingFeature(true) : bindingFeature;
            }
        }
    }

    protected void resolveMTOMFeature(Map<String, WebServiceFeature> featureMap) {
        if (featureMap.containsKey(MTOMFeature.ID)) {
            MTOMFeature mtom = (MTOMFeature) featureMap.get(MTOMFeature.ID);
            if (mtom.isEnabled()) {
                if (bindingId.equals(HTTPBinding.HTTP_BINDING)) {
                    throw new WebServiceException("Attempting to use the MTOMFeature with and HTTPBinding is an Error");
                } else if (! (bindingId.equals(SOAPBinding.SOAP11HTTP_MTOM_BINDING)
                        || bindingId.equals(SOAPBinding.SOAP12HTTP_MTOM_BINDING))) {  //where to write to - for now System.out
                    //log here- conflict between feature and bindingID. MTOMFeature has higher priority and
                    System.out.println("MTOMFeature is Enabled but the BindingID " + bindingId + " " + "is not a valid MTOM ID");
                    System.out.println("Overiding the bindingId " + bindingId + " MTOM remains turned on.");
                } else if (!mtom.isEnabled()) {
                    System.out.println("MTOMFeature is Disabled but the BindingID is MTOM, " + bindingId + " .");
                    System.out.println("Overiding the bindingId " + bindingId + " MTOM remains turned off.");
                }
            }
        } else { //featureMap does not contain MTOMFeature
            //is bindingId set
            if (bindingId.equals(SOAPBinding.SOAP11HTTP_MTOM_BINDING)
                    || bindingId.equals(SOAPBinding.SOAP12HTTP_MTOM_BINDING)) {
                //explicitly set the MTOM feature
                MTOMFeature mtomFeature = new MTOMFeature(true, 0);   //what is the default threshold?
                featureMap.put(MTOMFeature.ID, mtomFeature);
            }

        }
    }
  */

    protected static Map<String, WebServiceFeature> fillMap(WebServiceFeature[] webServiceFeatures) {
        HashMap<String, WebServiceFeature> featureMap = new HashMap<String, WebServiceFeature>(5);
        if (webServiceFeatures != null)
            for (int i = 0; i < webServiceFeatures.length; i++) {
                featureMap.put(webServiceFeatures[i].getID(), webServiceFeatures[i]);
            }
        return featureMap;
    }
}

