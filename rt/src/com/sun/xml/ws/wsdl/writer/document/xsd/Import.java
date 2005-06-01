/*
 * $Id: Import.java,v 1.1 2005-06-01 00:12:53 kohlert Exp $
 */

/*
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.xml.ws.wsdl.writer.document.xsd;

import com.sun.xml.txw2.TypedXmlWriter;
import com.sun.xml.txw2.annotation.XmlAttribute;
import com.sun.xml.txw2.annotation.XmlElement;
import com.sun.xml.ws.wsdl.writer.document.Documented;
import com.sun.xml.ws.wsdl.writer.document.*;

@XmlElement("import")
public interface Import
    extends TypedXmlWriter, Documented
{


    @XmlAttribute
    public com.sun.xml.ws.wsdl.writer.document.xsd.Import schemaLocation(String value);

    @XmlAttribute
    public com.sun.xml.ws.wsdl.writer.document.xsd.Import namespace(String value);

}
