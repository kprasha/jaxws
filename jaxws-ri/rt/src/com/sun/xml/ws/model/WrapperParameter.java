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

import com.sun.xml.bind.api.TypeReference;
import com.sun.xml.ws.api.model.Mode;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ParameterImpl} that represents a wrapper,
 * which is a parameter that consists of multiple nested {@link ParameterImpl}s
 * within.
 *
 * <p>
 * Java method parameters represented by nested {@link ParameterImpl}s will be
 * packed into a "wrapper bean" and it becomes the {@link ParameterImpl} for the
 * body.
 *
 * <p>
 * This parameter is only used for the {@link com.sun.xml.ws.api.model.ParameterBinding#BODY} binding.
 * 
 * @author Vivek Pandey
 */
public class WrapperParameter extends ParameterImpl {
    // TODO: wrapper parameter doesn't use 'typeRef' --- it only uses tag name.
    public WrapperParameter(AbstractSEIModelImpl rtModel, TypeReference typeRef, Mode mode, int index) {
        super(rtModel, typeRef, mode, index);
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
     * @param wrapperChildren
     *            The wrapperChildren to set.
     */
    public void addWrapperChildren(List<ParameterImpl> wrapperChildren) {
        this.wrapperChildren.addAll(wrapperChildren);
    }

    /**
     * @param wrapperChild
     */
    public void addWrapperChild(ParameterImpl wrapperChild) {
        wrapperChildren.add(wrapperChild);
    }

    public void clear(){
        wrapperChildren.clear();
    }

    protected final List<ParameterImpl> wrapperChildren = new ArrayList<ParameterImpl>();
}
