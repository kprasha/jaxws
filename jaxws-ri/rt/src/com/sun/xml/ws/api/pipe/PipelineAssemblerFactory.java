package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.client.dispatch.rearch.StandalonePipeAssembler;

import javax.xml.ws.soap.SOAPBinding;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates {@link PipelineAssembler}.
 *
 * <p>
 * To create a pipeline,
 * the JAX-WS runtime locates {@link PipelineAssemblerFactory}s through
 * the <tt>META-INF/services/com.sun.xml.ws.api.pipe.PipelineAssemblerFactory</tt> files.
 * Factories found are checked to see if it supports the given binding ID one by one,
 * and the first valid {@link PipelineAssembler} returned will be used to create
 * a pipeline.
 *
 * <p>
 * TODO: is bindingId really extensible? for this to be extensible,
 * someone seems to need to hook into WSDL parsing.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class PipelineAssemblerFactory {
    /**
     * Creates a {@link PipelineAssembler} applicable for the given binding ID.
     *
     * @param bindingId
     *      The binding ID for which a pipeline will be created,
     *      such as {@link SOAPBinding#SOAP11HTTP_BINDING}.
     *
     * @return
     *      null if this factory doesn't recognize the given binding ID.
     */
    public abstract PipelineAssembler doCreate(String bindingId);

    /**
     * Locates {@link PipelineAssemblerFactory}s and create
     * a suitable {@link PipelineAssembler}.
     *
     * @return
     *      null if no corresponding {@link PipelineAssemblerFactory}
     *      was found.
     */
    public static PipelineAssembler create(ClassLoader classLoader, String bindingId) {
        // TODO: think about caching

        String serviceId = "META-INF/services/" + PipelineAssemblerFactory.class.getName();

        // used to avoid creating the same instance twice
        Set<String> classNames = new HashSet<String>();

        logger.fine("Looking for "+serviceId+" for add-ons");

        // try to find services in CLASSPATH
        try {
            Enumeration<URL> e = classLoader.getResources(serviceId);

            while(e.hasMoreElements()) {
                URL url = e.nextElement();
                BufferedReader reader=null;

                logger.fine("Checking "+url+" for an add-on");

                try {
                    reader = new BufferedReader(new InputStreamReader(url.openStream(),"UTF-8"));
                    String impl;
                    while((impl = reader.readLine())!=null ) {
                        // try to instanciate the object
                        impl = impl.trim();
                        if(impl.startsWith("#"))
                            continue;       // comment line

                        if(classNames.add(impl)) {
                            Class implClass = classLoader.loadClass(impl);
                            if(!PipelineAssemblerFactory.class.isAssignableFrom(implClass)) {
                                logger.fine(impl+" is not a subclass of PipelineAssemblerFactory. Skipping");
                                continue;
                            }
                            logger.fine("Attempting to instanciate "+impl);
                            PipelineAssemblerFactory factory = (PipelineAssemblerFactory)implClass.newInstance();
                            PipelineAssembler assembler = factory.doCreate(bindingId);
                            if(assembler!=null) {
                                logger.fine(impl+" successfully created "+assembler);
                                return assembler;
                            }
                            logger.fine(impl+" didn't recognize "+bindingId);
                        }
                    }
                    reader.close();
                } catch( Exception ex ) {
                    // let it go.
                    logger.log(Level.FINE, "Failed to process "+url, ex);
                    if( reader!=null ) {
                        try {
                            reader.close();
                        } catch( IOException ex2 ) {
                        }
                    }
                }
            }
        } catch( Throwable e ) {
            logger.log(Level.FINE, "Failed to locate PipelineAssemblerFactory", e);
        }

        // default binding IDs that are known
        // TODO: replace this with proper ones
        return new StandalonePipeAssembler();
    }

    private static final Logger logger = Logger.getLogger(PipelineAssemblerFactory.class.getName());
}
