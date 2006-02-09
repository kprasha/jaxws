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

package com.sun.xml.ws.transport.http.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The JAX-WS dispatcher servlet.
 *
 * <p>
 * It really just forwards processing to {@link WSServletDelegate}.
 *
 * @author WS Development Team
 */
public final class WSServlet extends HttpServlet {

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        delegate = (WSServletDelegate) servletConfig.getServletContext().getAttribute(JAXWS_RI_RUNTIME_INFO);
    }

    protected void doPost( HttpServletRequest request, HttpServletResponse response) {
        if (delegate != null) {
            delegate.doPost(request,response,getServletContext());
        }
    }

    protected void doGet( HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        if (delegate != null) {
            delegate.doGet(request,response,getServletContext());
        }
    }

    private WSServletDelegate delegate = null;

    /**
     * {@link WSServletDelegate}.
     */
    public static final String JAXWS_RI_RUNTIME_INFO =
        "com.sun.xml.ws.server.http.servletDelegate";
    public static final String JAXWS_RI_PROPERTY_PUBLISH_WSDL =
        "com.sun.xml.ws.server.http.publishWSDL";
    public static final String JAXWS_RI_PROPERTY_PUBLISH_MODEL =
        "com.sun.xml.ws.server.http.publishModel";
    public static final String JAXWS_RI_PROPERTY_PUBLISH_STATUS_PAGE =
        "com.sun.xml.ws.server.http.publishStatusPage";

}
