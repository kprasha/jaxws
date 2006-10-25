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

package com.sun.xml.ws.binding;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.developer.MemberSubmissionAddressingFeature;
import com.sun.xml.ws.api.pipe.Codec;
import com.sun.xml.ws.client.HandlerConfiguration;
import com.sun.xml.ws.model.RuntimeModelerException;
import com.sun.xml.ws.resources.ModelerMessages;
import com.sun.xml.ws.resources.ServerMessages;
import com.sun.xml.ws.server.ServerRtException;

import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.MTOMFeature;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instances are created by the service, which then
 * sets the handler chain on the binding impl.
 * <p/>
 * <p>This class is made abstract as we dont see a situation when a BindingImpl has much meaning without binding id.
 * IOw, for a specific binding there will be a class extending BindingImpl, for example SOAPBindingImpl.
 * <p/>
 * <p>The spi Binding interface extends Binding.
 *
 * @author WS Development Team
 */
public abstract class BindingImpl implements WSBinding {
    private HandlerConfiguration handlerConfig;
    private final BindingID bindingId;
    // Features that are set(enabled/disabled) on the binding
    private  WebServiceFeatureList features = new WebServiceFeatureList();
    /**
     * Computed from {@link #features} by {@link #updateCache()}
     * to make {@link #getAddressingVersion()} faster.
     * // TODO: remove this constant value after debugging
     */
    private AddressingVersion addressingVersion = null;

    protected BindingImpl(BindingID bindingId) {
        this.bindingId = bindingId;
        setHandlerConfig(createHandlerConfig(Collections.<Handler>emptyList()));
    }

    public
    @NotNull
    List<Handler> getHandlerChain() {
        return handlerConfig.getHandlerChain();
    }

    public HandlerConfiguration getHandlerConfig() {
        return handlerConfig;
    }


    /**
     * Sets the handlers on the binding and then
     * sorts the handlers in to logical and protocol handlers.
     * Creates a new HandlerConfiguration object and sets it on the BindingImpl.
     */
    public void setHandlerChain(List<Handler> chain) {
        setHandlerConfig(createHandlerConfig(chain));
    }

    /**
     * This is called when ever Binding.setHandlerChain() or SOAPBinding.setRoles()
     * is called.
     * This sorts out the Handlers into Logical and SOAP Handlers and
     * sets the HandlerConfiguration.
     */
    protected void setHandlerConfig(HandlerConfiguration handlerConfig) {
        this.handlerConfig = handlerConfig;
    }

    protected abstract HandlerConfiguration createHandlerConfig(List<Handler> handlerChain);

    public
    @NotNull
    BindingID getBindingId() {
        return bindingId;
    }

    public final SOAPVersion getSOAPVersion() {
        return bindingId.getSOAPVersion();
    }

    public AddressingVersion getAddressingVersion() {
        return addressingVersion;
    }

    public final
    @NotNull
    Codec createCodec() {
        return bindingId.createEncoder(this);
    }

    public static BindingImpl create(@NotNull BindingID bindingId) {
        if (bindingId.equals(BindingID.XML_HTTP))
            return new HTTPBindingImpl();
        else
            return new SOAPBindingImpl(bindingId);
    }

    public static BindingImpl create(@NotNull BindingID bindingId, WebServiceFeature[] features) {
        if (bindingId.equals(BindingID.XML_HTTP))
            return new HTTPBindingImpl();
        else
            return new SOAPBindingImpl(bindingId, features);
    }

    public static WSBinding getDefaultBinding() {
        return new SOAPBindingImpl(BindingID.SOAP11_HTTP);
    }

    public boolean isMTOMEnabled() {
        return false;//default
    }

    public void setMTOMEnabled(boolean value) {
    }

    public String getBindingID() {
        return bindingId.toString();
    }

    public WebServiceFeature getFeature(String featureId) {
        if (featureId == null)
            return null;
        return features.getFeature(featureId);
    }

    public @Nullable <F extends WebServiceFeature> F getFeature(@NotNull Class<F> featureType){
        return features.getFeature(featureType);
    }

    public boolean isFeatureEnabled(String featureId) {
        return features.isFeatureEnabled(featureId);
    }

    public boolean isFeatureEnabled(@NotNull Class<? extends WebServiceFeature> feature){
        return features.isFeatureEnabled(feature);
    }

    private void updateCache() {
//        addressingVersion = AddressingVersion.W3C;
        if (isFeatureEnabled(AddressingFeature.class))
            addressingVersion = AddressingVersion.W3C;
        else if (isFeatureEnabled(MemberSubmissionAddressingFeature.class))
            addressingVersion = AddressingVersion.MEMBER;
        else
            addressingVersion = null;
    }

    /**
     * Make sure updateCache() is called after a feature is enabled
     *  
     * @param feature
     */
    private <F extends WebServiceFeature> void enableFeature(@NotNull F feature) {
        features.addFeature(feature);
    }

    public void setFeatures(WebServiceFeature... newFeatures) {
        if (newFeatures != null) {
            for (WebServiceFeature f : newFeatures) {
                enableFeature(f);
            }
        }
        updateCache();
    }

    public void addFeature(@NotNull WebServiceFeature newFeature) {
        enableFeature(newFeature);
        updateCache();
    }

    //what does this mean
    public boolean isAddressingEnabled() {
        return addressingVersion != null;
    }

    /**
     * @param ddBindingId :binding id explicitlyspecified in the DeploymentDescriptor or parameter
     * @param implClass : Endpoint Implementation class
     * @param mtomEnabled : represents mtom-enabled attribute in DD
     * @param mtomThreshold : threshold value specified in DD
     * @param features  : WebServiceFeatures if any specified in DD
     *                  Currently no way to specify features in DD, so ignore it
     * @return WSBinding is returned resolving the various precendece rules
     */
    public static WSBinding create(String ddBindingId,Class implClass,
                                          String mtomEnabled, String mtomThreshold,
                                          WebServiceFeature[] features) {
        // Features specified through annotaion
        WebServiceFeatureList implFeatures = new WebServiceFeatureList(implClass);
        MTOMFeature mtomfeature = null;

        BindingID bindingID;
        if (ddBindingId != null) {
            bindingID = BindingID.parse(ddBindingId);
            if (bindingID.isMTOMEnabled() == null) {
                if (mtomEnabled != null) {
                    if (mtomThreshold != null)
                        mtomfeature = new MTOMFeature(Boolean.valueOf(mtomEnabled),
                                Integer.valueOf(mtomThreshold));
                    else
                        mtomfeature = new MTOMFeature(Boolean.valueOf(mtomEnabled));
                } else {
                    mtomfeature = (MTOMFeature) implFeatures.getFeature(MTOMFeature.ID);
                }
            } else if ((mtomEnabled != null) && !(Boolean.valueOf(mtomEnabled)== bindingID.isMTOMEnabled())) {
                throw new ServerRtException(ServerMessages.DD_MTOM_CONFLICT(ddBindingId, mtomEnabled));
            }
        } else {
            bindingID = BindingID.parse(implClass);
            // Since bindingID is coming from implclass,
            // mtom through Feature annotation or DD takes precendece
            if (mtomEnabled != null) {
                if (mtomThreshold != null)
                    mtomfeature = new MTOMFeature(Boolean.valueOf(mtomEnabled),
                            Integer.valueOf(mtomThreshold));
                else
                    mtomfeature = new MTOMFeature(Boolean.valueOf(mtomEnabled));
            } else {
                mtomfeature = (MTOMFeature) implFeatures.getFeature(MTOMFeature.ID);
                if ((bindingID.isMTOMEnabled() != null) && (mtomfeature != null)) {
                    //if both are specified , make sure they don't conflict
                    if (mtomfeature.isEnabled() != bindingID.isMTOMEnabled())
                        throw new RuntimeModelerException(
                                ModelerMessages.RUNTIME_MODELER_MTOM_CONFLICT(bindingID, mtomfeature.isEnabled()));
                }
            }
        }
        //If mtom is enabled thorugh bindingID, it will will be enabled on the binding
        BindingImpl binding = create(bindingID,implFeatures.getFeatures());
        if(mtomfeature != null) {
            // this will be non-null incase,
            // where mtom is controlled through higer precedence control
            // so, override it
            (binding).enableFeature(mtomfeature);
        }
        return binding;
    }

}
