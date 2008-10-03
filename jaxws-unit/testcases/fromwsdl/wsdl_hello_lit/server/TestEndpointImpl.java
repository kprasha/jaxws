/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fromwsdl.wsdl_hello_lit.server;

import java.util.List;

/**
 * @author Vivek Pandey
 */
@javax.jws.WebService(endpointInterface = "fromwsdl.wsdl_hello_lit.server.Hello")

public class TestEndpointImpl implements Hello {
    public HelloResponse hello(Hello_Type req) {
        System.out.println("Hello_PortType_Impl received: " + req.getArgument() +
                ", " + req.getExtra());
        HelloResponse resp = new HelloResponse();
        resp.setName("vivek");
        resp.setArgument(req.getArgument());
        resp.setExtra(req.getExtra());
        return resp;
    }

    public VoidType voidTest(VoidType req) {
        if (req == null)
            return null;
        return new VoidType();
    }

    public void echoArray(javax.xml.ws.Holder<NameType> name) {
    }

    public void echoArray1(javax.xml.ws.Holder<NameType> name) {
        NameType resp = name.value;
        resp.getName().add("EA");
    }

    public void echoArray2(javax.xml.ws.Holder<NameType> name) {
    }

    public void echoArray3(javax.xml.ws.Holder<List<String>> name) {

    }

    public NameType1 echoArray4(NameType1 request) {
        NameType1 resp = new NameType1();
        HelloType ht = new HelloType();
        ht.setArgument("arg1");
        ht.setExtra("extra1");


        HelloType ht1 = new HelloType();
        ht1.setArgument("arg2");
        ht1.setExtra("extra2");
        resp.getName().add(ht);
        resp.getName().add(ht1);
        return resp;
    }

    public String testKeyword(String _this) {
        return _this + " World!";
    }
}