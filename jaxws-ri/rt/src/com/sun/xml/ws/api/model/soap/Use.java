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
 * Models <code>wsdl:binding/wsdl:operation/wsdl:input/soap:body@use</code> attribute
 * value or {@link javax.jws.soap.SOAPBinding.Use}, the possible values are
 * <code>literal</code> or <code>encoded</code>.
 *<pre>
 * Example wsdl:
 * &lt;wsdl:binding name="HelloBinding" type="tns:Hello">
 *   &lt;soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>
 *   &lt;wsdl:operation name="echoData">
 *       &lt;soap12:operation soapAction=""/>
 *       &lt;wsdl:input>
 *           &lt;soap12:body <b>use="literal"</b>/>
 * ...
 * </pre>
 * @author Vivek Pandey
 */
public enum Use {
    LITERAL(0), ENCODED(1);

    private Use(int use){
        this.use = use;
    }

    public int value() {
        return use;
    }
    private final int use;
}
