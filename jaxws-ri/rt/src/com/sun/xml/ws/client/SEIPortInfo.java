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

import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.model.SOAPSEIModel;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.binding.SOAPBindingImpl;
import com.sun.istack.NotNull;


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
        BindingImpl bindingImpl = super.createBinding();
        if(bindingImpl instanceof SOAPBindingImpl) {
            ((SOAPBindingImpl)bindingImpl).setPortKnownHeaders(model.getKnownHeaders());
        }
        return bindingImpl;
    }
}