package com.sun.xml.ws.util.pipe;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterPipeImpl;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.PrintStream;
import java.lang.reflect.Constructor;

/**
 * {@link Pipe} that dumps messages that pass through.
 *
 * @author Kohsuke Kawaguchi
 */
public class DumpPipe extends AbstractFilterPipeImpl {
    private final PrintStream out;

    private final XMLOutputFactory staxOut;

    /**
     * @param out
     *      The output to send dumps to.
     * @param next
     *      The next {@link Pipe} in the pipeline.
     */
    public DumpPipe(PrintStream out, Pipe next) {
        super(next);
        this.out = out;
        this.staxOut = XMLOutputFactory.newInstance();
        staxOut.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,true);
    }

    /**
     * Copy constructor.
     */
    private DumpPipe(DumpPipe that, PipeCloner cloner) {
        super(that,cloner);
        this.out = that.out;
        this.staxOut = that.staxOut;
    }

    public Packet process(Packet packet) {
        dump("request",packet);
        Packet reply = next.process(packet);
        dump("response",reply);
        return reply;
    }

    private void dump(String header, Packet packet) {
        out.println("====["+header+"]====");
        if(packet.getMessage()==null)
            out.println("(none)");
        else
            try {
                XMLStreamWriter writer = staxOut.createXMLStreamWriter(new PrintStream(out) {
                    public void close() {
                        // noop
                    }
                });
                writer = createIndenter(writer);
                packet.getMessage().copy().writeTo(writer);
                writer.close();
            } catch (XMLStreamException e) {
                e.printStackTrace(out);
            }
        out.println("============");
    }

    /**
     * Wraps {@link XMLStreamWriter} by an indentation engine if possible.
     *
     * <p>
     * We can do this only when we have <tt>stax-utils.jar</tt> in the classpath.
     */
    private XMLStreamWriter createIndenter(XMLStreamWriter writer) {
        try {
            Class clazz = getClass().getClassLoader().loadClass("javanet.staxutils.IndentingXMLStreamWriter");
            Constructor c = clazz.getConstructor(XMLStreamWriter.class);
            writer = (XMLStreamWriter)c.newInstance(writer);
        } catch (Exception e) {
            // if stax-utils.jar is not in the classpath, this will fail
            // so, we'll just have to do without indentation
            if(!warnStaxUtils) {
                warnStaxUtils = true;
                out.println("WARNING: put stax-utils.jar to the classpath to indent the dump output");
            }
        }
        return writer;
    }


    public Pipe copy(PipeCloner cloner) {
        return new DumpPipe(this,cloner);
    }

    public void preDestroy() {
        // noop
    }

    private static boolean warnStaxUtils;
}
