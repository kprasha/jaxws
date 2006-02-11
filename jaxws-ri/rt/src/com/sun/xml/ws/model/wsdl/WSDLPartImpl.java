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
package com.sun.xml.ws.model.wsdl;

import com.sun.xml.ws.api.model.ParameterBinding;
import com.sun.xml.ws.api.model.wsdl.WSDLPart;
import com.sun.xml.ws.api.model.wsdl.WSDLPartDescriptor;

import javax.xml.namespace.QName;

/**
 * Implementation of {@link WSDLPart}
 *
 * @author Vivek Pandey
 */
public final class WSDLPartImpl implements WSDLPart {
    private final String name;
    private ParameterBinding binding;
    private int index;
    private final WSDLPartDescriptor descriptor;

    public WSDLPartImpl(String partName, int index, WSDLPartDescriptor descriptor) {
        this.name = partName;
        this.binding = ParameterBinding.UNBOUND;
        this.index = index;
        this.descriptor = descriptor;

    }

    public String getName() {
        return name;
    }

    public ParameterBinding getBinding() {
        return binding;
    }

    public void setBinding(ParameterBinding binding) {
        this.binding = binding;
    }

    public int getIndex() {
        return index;
    }

    //need to set the index in case of rpclit to reorder the body parts
    public void setIndex(int index){
        this.index = index;
    }

    boolean isBody(){
        return binding.isBody();
    }

    public WSDLPartDescriptor getDescriptor() {
        return descriptor;
    }
}
