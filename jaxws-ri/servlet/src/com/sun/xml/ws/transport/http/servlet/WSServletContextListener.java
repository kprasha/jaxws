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
import com.sun.xml.ws.resources.WsservletMessages;
import com.sun.xml.ws.transport.http.DeploymentDescriptorParser;
import com.sun.xml.ws.transport.http.HttpAdapter;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Parses {@code sun-jaxws.xml} and sets up
 * {@link HttpAdapter}s for all deployed endpoints.
 *
 * <p>
 * This code is the entry point at the server side.
 *
 * @author WS Development Team
 */
public final class WSServletContextListener
    implements ServletContextAttributeListener, ServletContextListener {

    private WSServletDelegate delegate;


    public void attributeAdded(ServletContextAttributeEvent event) {
    }

    public void attributeRemoved(ServletContextAttributeEvent event) {
    }

    public void attributeReplaced(ServletContextAttributeEvent event) {
    }

    public void contextDestroyed(ServletContextEvent event) {
        if (delegate != null) { // the deployment might have failed.
            delegate.destroy();
        }

        if (logger.isLoggable(Level.INFO)) {
            logger.info(WsservletMessages.LISTENER_INFO_DESTROY());
        }
    }

    public void contextInitialized(ServletContextEvent event) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(WsservletMessages.LISTENER_INFO_INITIALIZE());
        }
        ServletContext context = event.getServletContext();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        try {
            // Parse the descriptor file and build endpoint infos
            DeploymentDescriptorParser<ServletAdapter> parser = new DeploymentDescriptorParser<ServletAdapter>(
                classLoader,new ServletResourceLoader(context),null,ServletAdapter.FACTORY);
            InputStream is = context.getResourceAsStream(JAXWS_RI_RUNTIME);
            List<ServletAdapter> adapters = parser.parse(is);

            delegate = new WSServletDelegate(adapters,context);

            context.setAttribute(WSServlet.JAXWS_RI_RUNTIME_INFO,delegate);
            
        } catch (Throwable e) {
            logger.log(Level.SEVERE,
                WsservletMessages.LISTENER_PARSING_FAILED(e),e);
            context.removeAttribute(WSServlet.JAXWS_RI_RUNTIME_INFO);
            throw new WSServletException("listener.parsingFailed", e);
        }
    }
    
    private static final String JAXWS_RI_RUNTIME = "/WEB-INF/sun-jaxws.xml";

    private static final Logger logger =
        Logger.getLogger(
            com.sun.xml.ws.util.Constants.LoggingDomain + ".server.http");
}
