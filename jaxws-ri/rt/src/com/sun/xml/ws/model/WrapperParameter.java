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
package com.sun.xml.ws.model;

import com.sun.xml.bind.api.CompositeStructure;
import com.sun.xml.bind.api.TypeReference;
import com.sun.xml.ws.api.model.JavaMethod;
import com.sun.xml.ws.api.model.ParameterBinding;
import com.sun.xml.ws.encoding.jaxb.RpcLitPayload;

import javax.jws.WebParam.Mode;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ParameterImpl} that represents a wrapper,
 * which is a parameter that consists of multiple nested {@link ParameterImpl}s
 * within, which together form a body part.
 *
 * <p>
 * Java method parameters represented by nested {@link ParameterImpl}s will be
 * packed into a "wrapper bean" and it becomes the {@link ParameterImpl} for the
 * body.
 *
 * <p>
 * This parameter is only used for the {@link ParameterBinding#BODY} binding.
 * Other parameters that bind to other parts (such as headers or unbound)
 * will show up directly under {@link JavaMethod}.
 * 
 * @author Vivek Pandey
 */
public class WrapperParameter extends ParameterImpl {
    protected final List<ParameterImpl> wrapperChildren = new ArrayList<ParameterImpl>();

    // TODO: wrapper parameter doesn't use 'typeRef' --- it only uses tag name.
    public WrapperParameter(JavaMethodImpl parent, TypeReference typeRef, Mode mode, int index) {
        super(parent, typeRef, mode, index);
    }

    /**
     *
     * @deprecated
     *      Why are you calling a method that always return true?
     */
    @Override
    public boolean isWrapperStyle() {
        return true;
    }

    /**
     * @return Returns the wrapperChildren.
     */
    public List<ParameterImpl> getWrapperChildren() {
        return wrapperChildren;
    }

    /**
     * Adds a new child parameter.
     * 
     * @param wrapperChild
     */
    public void addWrapperChild(ParameterImpl wrapperChild) {
        wrapperChildren.add(wrapperChild);
        // must bind to body. see class javadoc
        assert wrapperChild.getBinding()== ParameterBinding.BODY;
    }

    public void clear(){
        wrapperChildren.clear();
    }

    @Override
    void fillTypes(List<TypeReference> types) {
        // RpcLitPayload is for supporting legacy server-side code.
        // this code should be cleaned up once the server moves to the new architecture
        if(getTypeReference().type == RpcLitPayload.class) {
            // legacy support. to be removed eventually
            for (ParameterImpl p : wrapperChildren)
                p.fillTypes(types);
        } else {
            super.fillTypes(types);
            if(getParent().getBinding().isRpcLit()) {
                // for rpc/lit, we need to individually marshal/unmarshal wrapped values,
                // so their TypeReference needs to be collected
                assert getTypeReference().type==CompositeStructure.class;
                for (ParameterImpl p : wrapperChildren)
                    p.fillTypes(types);
            }
        }
    }
}
