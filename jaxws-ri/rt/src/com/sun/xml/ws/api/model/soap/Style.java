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
package com.sun.xml.ws.api.model.soap;

/**
 * Models <code>wsdl:binding/soap:binding@style</code>
 * attribute value or {@link javax.jws.soap.SOAPBinding.Style}.
 * The possible values are <code>document</code> or <code>rpc</code>.
 * <pre>
 * For example:
 * &lt;wsdl:binding name="HelloBinding" type="tns:Hello">
 *   &lt;soap:binding <b>style="document"</b> transport="http://schemas.xmlsoap.org/soap/http"/>
 * ...
 * </pre>
 *
 * @author Vivek Pandey
 */
public enum Style {
    DOCUMENT(0), RPC(1);

    private Style(int style) {
        this.style = style;
    }

    public int value() {
        return style;
    }

    private final int style;
}
