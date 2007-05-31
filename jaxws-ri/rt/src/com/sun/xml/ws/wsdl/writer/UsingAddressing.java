/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
/*
 * $Id: UsingAddressing.java,v 1.1.2.3.2.1 2007-05-31 23:41:06 ofung Exp $
 */

package com.sun.xml.ws.wsdl.writer;

import com.sun.xml.txw2.TypedXmlWriter;
import com.sun.xml.txw2.annotation.XmlAttribute;
import com.sun.xml.txw2.annotation.XmlElement;
import com.sun.xml.ws.addressing.W3CAddressingConstants;
import com.sun.xml.ws.wsdl.writer.document.StartWithExtensionsType;

/**
 * @author Arun Gupta
 */
@XmlElement(value = W3CAddressingConstants.WSA_NAMESPACE_WSDL_NAME,
            ns = W3CAddressingConstants.WSAW_USING_ADDRESSING_NAME)
public interface UsingAddressing extends TypedXmlWriter, StartWithExtensionsType {
    @XmlAttribute(value = "required", ns = "http://schemas.xmlsoap.org/wsdl/")
    public void required(boolean b);
}
