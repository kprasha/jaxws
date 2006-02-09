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

package com.sun.tools.ws.processor.generator;

import com.sun.codemodel.JMethod;
import com.sun.tools.ws.api.TJavaGeneratorExtension;
import com.sun.tools.ws.api.wsdl.TWSDLExtensible;
import com.sun.tools.ws.api.wsdl.TWSDLOperation;

/**
 * @author Arun Gupta
 */
public final class JavaGeneratorExtensionFacade extends TJavaGeneratorExtension {
    private final TJavaGeneratorExtension[] extensions;

    JavaGeneratorExtensionFacade(TJavaGeneratorExtension... extensions) {
        assert extensions != null;
        this.extensions = extensions;
    }

    public void writeMethodAnnotations(TWSDLOperation wsdlOperation, JMethod jMethod) {
        for (TJavaGeneratorExtension e : extensions) {
            e.writeMethodAnnotations(wsdlOperation, jMethod);
        }
    }
}
