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
import com.sun.xml.ws.api.model.wsdl.Part;
import com.sun.xml.ws.api.model.wsdl.PartDescriptor;

/**
 * Implementation of {@link Part}
 *
 * @author Vivek Pandey
 */
public final class PartImpl implements Part {
    private final String name;
    private final ParameterBinding binding;
    private final int index;

    /**
     * The constructor is used when the wsdl:part order is known from the abstract wsdl:portType. This constructor
     * should be called when atleast all of wsdl:portType, wsdl:binding, wsdl:service are parsed. IOW, this should
     * be called after all the WSDL is parsed - during post processing.
     * @param name
     * @param binding
     * @param index
     * @see {@link BoundPortTypeImpl#finalizeRpcLitBinding()}
     */
    public PartImpl(String name, ParameterBinding binding, int index) {
        this.name = name;
        this.binding = binding;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public ParameterBinding getBinding() {
        return binding;
    }

    public int getIndex() {
        return index;
    }

    //TODO
    public PartDescriptor getDescriptor() {
        throw new UnsupportedOperationException();
    }
}
