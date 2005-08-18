/**
 * $Id: BodyType.java,v 1.2 2005-08-18 19:11:48 kohlert Exp $
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.xml.ws.wsdl.writer.document.soap;

import com.sun.xml.txw2.TypedXmlWriter;
import com.sun.xml.txw2.annotation.XmlAttribute;

/**
 *
 * @author WS Development Team
 */
public interface BodyType
    extends TypedXmlWriter
{


    @XmlAttribute
    public com.sun.xml.ws.wsdl.writer.document.soap.BodyType encodingStyle(String value);

    @XmlAttribute
    public com.sun.xml.ws.wsdl.writer.document.soap.BodyType namespace(String value);

    @XmlAttribute
    public com.sun.xml.ws.wsdl.writer.document.soap.BodyType use(String value);

    @XmlAttribute
    public com.sun.xml.ws.wsdl.writer.document.soap.BodyType parts(String value);

}
