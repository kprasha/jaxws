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

package com.sun.tools.ws.wsdl.document;

import javax.xml.namespace.QName;
import com.sun.tools.ws.wsdl.framework.EntityAction;
import com.sun.tools.ws.wsdl.framework.ExtensibilityHelper;
import com.sun.tools.ws.api.wsdl.TWSDLExtensible;
import com.sun.tools.ws.api.wsdl.TWSDLExtension;
import com.sun.tools.ws.wsdl.framework.Entity;

/**
 * Entity corresponding to the "input" child element of a binding operation.
 *
 * @author WS Development Team
 */
public class BindingInput extends Entity implements TWSDLExtensible {

    public BindingInput() {
        _helper = new ExtensibilityHelper();
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public QName getElementName() {
        return WSDLConstants.QNAME_INPUT;
    }

    public Documentation getDocumentation() {
        return _documentation;
    }

    public void setDocumentation(Documentation d) {
        _documentation = d;
    }

    public String getNameValue() {
        return getName();
    }

    public String getNamespaceURI() {
        return getParent().getNamespaceURI();
    }

    public QName getWSDLElementName() {
        return getElementName();
    }

    public void addExtension(TWSDLExtension e) {
        _helper.addExtension(e);
    }

    public Iterable<TWSDLExtension> extensions() {
        return _helper.extensions();
    }

    public TWSDLExtensible getParent() {
        return parent;
    }

    public void withAllSubEntitiesDo(EntityAction action) {
        _helper.withAllSubEntitiesDo(action);
    }

    public void accept(WSDLDocumentVisitor visitor) throws Exception {
        visitor.preVisit(this);
        _helper.accept(visitor);
        visitor.postVisit(this);
    }

    public void validateThis() {
    }

    private ExtensibilityHelper _helper;
    private Documentation _documentation;
    private String _name;

    public void setParent(TWSDLExtensible parent) {
        this.parent = parent;
    }

    private TWSDLExtensible parent;
}
