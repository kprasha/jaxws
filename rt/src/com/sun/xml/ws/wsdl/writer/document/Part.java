/**
 * $Id: Part.java,v 1.2 2005-08-18 19:11:44 kohlert Exp $
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.xml.ws.wsdl.writer.document;

import javax.xml.namespace.QName;
import com.sun.xml.txw2.TypedXmlWriter;
import com.sun.xml.txw2.annotation.XmlAttribute;
import com.sun.xml.txw2.annotation.XmlElement;
import com.sun.xml.ws.wsdl.writer.document.OpenAtts;

/**
 *
 * @author WS Development Team
 */
@XmlElement("part")
public interface Part
    extends TypedXmlWriter, OpenAtts
{


    @XmlAttribute
    public com.sun.xml.ws.wsdl.writer.document.Part element(QName value);

    @XmlAttribute
    public com.sun.xml.ws.wsdl.writer.document.Part type(QName value);

    @XmlAttribute
    public com.sun.xml.ws.wsdl.writer.document.Part name(String value);

}
