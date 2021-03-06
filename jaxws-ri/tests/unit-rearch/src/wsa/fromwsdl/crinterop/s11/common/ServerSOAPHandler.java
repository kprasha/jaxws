/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package wsa.fromwsdl.crinterop.s11.common;

import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.WebServiceException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPBody;

import testutil.WsaBaseSOAPHandler;

/**
 * @author Arun Gupta
 */
public class ServerSOAPHandler extends WsaBaseSOAPHandler {
    protected void checkInboundActions(String oper, String action) {
        if (oper.equals("test1130") || oper.equals("test1131") ||
                oper.equals("test1132") || oper.equals("test1133") ||
                oper.equals("test1134") || oper.equals("test1150") ||
                oper.equals("fault-test1152")) {
            if (!action.equals(TestConstants.ECHO_IN_ACTION))
                throw new WebServiceException("Unexpected action received" + action);
        } else if (oper.equals("1100") || oper.equals("1101") ||
                oper.equals("1102") || oper.equals("1103") ||
                oper.equals("1104") || oper.equals("1106") ||
                oper.equals("1107") || oper.equals("1108")) {
            System.out.println("checking one-way actions");
            if (!action.equals(TestConstants.NOTIFY_ACTION))
                throw new WebServiceException("Unexpected action received" + action);
        }
    }

    @Override
    public boolean handleFault(SOAPMessageContext smc) {
        return true;
    }

    @Override
    protected String getOperationName(SOAPBody soapBody) throws SOAPException {
        return soapBody.getFirstChild().getTextContent();
    }
}
