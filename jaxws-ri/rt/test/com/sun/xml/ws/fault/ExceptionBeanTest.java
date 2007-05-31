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

package com.sun.xml.ws.fault;

import com.sun.xml.ws.developer.ServerSideException;
import com.sun.xml.ws.util.DOMUtil;
import com.sun.xml.ws.util.xml.XmlUtil;
import junit.framework.TestCase;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.net.URL;

/**
 * @author Kohsuke Kawaguchi
 */
public class ExceptionBeanTest extends TestCase {
    /**
     * Makes sure it round-trips.
     */
    public void test1() throws Exception {
        NullPointerException y = new NullPointerException();
        y.fillInStackTrace();

        Exception e = new Exception(y);
        e.fillInStackTrace();

        Document dom = DOMUtil.createDom();
        ExceptionBean.marshal(e,dom);

        XmlUtil.newTransformer().transform(new DOMSource(dom),new StreamResult(System.out));

        ServerSideException z = ExceptionBean.unmarshal(dom.getDocumentElement());
        z.printStackTrace(System.out);
    }

    /**
     * Unmarshal test to verify the on-the-wire format compatibility.
     */
    public void test2() throws Exception {
        URL res = getClass().getResource("exception.xml");

        Document d = DOMUtil.createDom();
        XmlUtil.newTransformer().transform(new StreamSource(res.toExternalForm()),new DOMResult(d));

        ServerSideException se = ExceptionBean.unmarshal(d.getDocumentElement());
        assertTrue(se!=null);
    }
}
