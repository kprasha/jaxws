package com.sun.xml.ws.api.streaming;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.streaming.XMLReaderException;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.InputSource;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * Factory for {@link XMLStreamReader}.
 *
 * <p>
 * This wraps {@link XMLInputFactory} and allows us to reuse {@link XMLStreamReader} instances
 * when appropriate.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class XMLStreamReaderFactory {
    
    /*
     * @TODO Make XMLStreamReaderFactory support multiple XMLStreamReader implementations at one time.
     *       Like Zephyr, FastInfoset etc
     */
    
    /**
     * Singleton instance.
     */
    private static volatile @NotNull XMLStreamReaderFactory theInstance;
    
    static {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        
        XMLStreamReaderFactory f=null;
        
        // this system property can be used to disable the pooling altogether,
        // in case someone hits an issue with pooling in the production system.
        if(!Boolean.getBoolean(XMLStreamReaderFactory.class.getName()+".noPool"))
            f = Zephyr.newInstance(xif);
        if(f==null)
            f = Default.newInstance(xif);
        
        theInstance = f;
    }
    
    /**
     * Overrides the singleton {@link XMLStreamReaderFactory} instance that
     * the JAX-WS RI uses.
     */
    public static void set(XMLStreamReaderFactory f) {
        if(f==null) throw new IllegalArgumentException();
        theInstance = f;
    }
    
    public static XMLStreamReaderFactory get() {
        return theInstance;
    }
    
    public static XMLStreamReader create(InputSource source, boolean rejectDTDs) {
        try {
            // Char stream available?
            if (source.getCharacterStream() != null) {
                return get().doCreate(source.getSystemId(), source.getCharacterStream(), rejectDTDs);
            }
            
            // Byte stream available?
            if (source.getByteStream() != null) {
                return get().doCreate(source.getSystemId(), source.getByteStream(), rejectDTDs);
            }
            
            // Otherwise, open URI
            return get().doCreate(source.getSystemId(), new URL(source.getSystemId()).openStream(),rejectDTDs);
        } catch (IOException e) {
            throw new XMLReaderException("stax.cantCreate",e);
        }
    }
    
    public static XMLStreamReader create(@Nullable String systemId, InputStream in, boolean rejectDTDs) {
        return get().doCreate(systemId,in,rejectDTDs);
    }
    
    public static XMLStreamReader create(@Nullable String systemId, Reader reader, boolean rejectDTDs) {
        return get().doCreate(systemId,reader,rejectDTDs);
    }
    
    /**
     * Should be invoked when the code finished using an {@link XMLStreamReader}.
     *
     * <p>
     * If the recycled instance implements {@link RecycleAware},
     * {@link RecycleAware#onRecycled()} will be invoked to let the instance
     * know that it's being recycled.
     *
     * <p>
     * It is not a hard requirement to call this method on every {@link XMLStreamReader}
     * instance. Not doing so just reduces the performance by throwing away
     * possibly reusable instances. So the caller should always consider the effort
     * it takes to recycle vs the possible performance gain by doing so.
     *
     * <p>
     * This method may be invked by multiple threads concurrently.
     *
     * @param r
     *      The {@link XMLStreamReader} instance that the caller finished using.
     *      This could be any {@link XMLStreamReader} implementation, not just
     *      the ones that were created from this factory. So the implementation
     *      of this class needs to be aware of that.
     */
    public static void recycle(XMLStreamReader r) {
        get().doRecycle(r);
    }
    
    // implementations
    
    public abstract void registerXMLInputFactory(Class<? extends XMLStreamReader> clazz, XMLInputFactory factory);
    
    public abstract XMLStreamReader doCreate(String systemId, InputStream in, boolean rejectDTDs);
    
    public abstract XMLStreamReader doCreate(String systemId, Reader reader, boolean rejectDTDs);
    
    public abstract XMLStreamReader doCreate(String systemId, InputStream in, boolean rejectDTDs, Class<? extends XMLStreamReader> clazz);
    
    public abstract void doRecycle(XMLStreamReader r);
    
    /**
     * Interface that can be implemented by {@link XMLStreamReader} to
     * be notified when it's recycled.
     *
     * <p>
     * This provides a filtering {@link XMLStreamReader} an opportunity to
     * recycle its inner {@link XMLStreamReader}.
     */
    public interface RecycleAware {
        void onRecycled();
    }
    
    /**
     * {@link XMLStreamReaderFactory} implementation for SJSXP/JAXP RI.
     */
    public static final class Zephyr extends XMLStreamReaderFactory {
        private final Map<Class<?>, XMLInputFactory> xifMap;
        
        private final ThreadLocal<Map<Class<?>, XMLStreamReader>> pool = new ThreadLocal<Map<Class<?>, XMLStreamReader>>();
        
        /**
         * Sun StAX impl <code>XMLReaderImpl.setInputSource()</code> method via reflection.
         */
        private final Method setInputSourceMethod;
        
        /**
         * Sun StAX impl <code>XMLReaderImpl.reset()</code> method via reflection.
         */
        private final Method resetMethod;
        
        /**
         * The Sun StAX impl's {@link XMLStreamReader} implementation clas.
         */
        private final Class zephyrClass;
        
        /**
         * Creates {@link Zephyr} instance if the given {@link XMLInputFactory} is the one
         * from Zephyr.
         */
        public static @Nullable
                XMLStreamReaderFactory newInstance(XMLInputFactory xif) {
            // check if this is from Zephyr
            try {
                Class<?> clazz = xif.createXMLStreamReader(new StringReader("<foo/>")).getClass();
                
                if(!clazz.getName().startsWith("com.sun.xml.stream."))
                    return null;    // nope
                
                return new Zephyr(xif,clazz);
            } catch (NoSuchMethodException e) {
                return null;    // this factory is not for zephyr
            } catch (XMLStreamException e) {
                return null;    // impossible to fail to parse <foo/>, but anyway
            }
        }
        
        public Zephyr(XMLInputFactory xif, Class clazz) throws NoSuchMethodException {
            zephyrClass = clazz;
            setInputSourceMethod = clazz.getMethod("setInputSource", InputSource.class);
            resetMethod = clazz.getMethod("reset");
            
            try {
                // Turn OFF internal factory caching in Zephyr.
                // Santiago told me that this makes it thread-safe.
                xif.setProperty("reuse-instance", false);
            } catch (IllegalArgumentException e) {
                // falls through
            }
            
            xifMap = new HashMap<Class<?>, XMLInputFactory>(4);
            xifMap.put(clazz, xif);
        }
        
        
        public void doRecycle(XMLStreamReader r) {
            offer(r);
            
            if(r instanceof RecycleAware)
                ((RecycleAware)r).onRecycled();
        }
        
        public void registerXMLInputFactory(Class<? extends XMLStreamReader> clazz, XMLInputFactory factory) {
            xifMap.put(clazz, factory);
        }
        
        /*
         * Method returns XMLStreamReader either from pool, if any available, or creates a new one.
         * If XMLStreamReader is taken from pool, than systemId and in arguments are not used
         */
        public XMLStreamReader doCreate(String systemId, InputStream in, boolean rejectDTDs, Class<? extends XMLStreamReader> clazz) {
            try {
                XMLStreamReader xsr = fetch(clazz);
                if(xsr==null)
                    return xifMap.get(clazz).createXMLStreamReader(systemId,in);
                
                return xsr;
            } catch (XMLStreamException e) {
                throw new XMLReaderException("stax.cantCreate",e);
            }
        }
        
        /*
         * Method returns Zephyr XMLStreamReader either from pool, if any available, or creates a new one.
         * If instance is taken from pool - its state will be reset with new systemId and in values
         */
        public XMLStreamReader doCreate(String systemId, InputStream in, boolean rejectDTDs) {
            try {
                XMLStreamReader xsr = fetch(zephyrClass);
                if(xsr==null)
                    return xifMap.get(zephyrClass).createXMLStreamReader(systemId,in);
                
                // try re-using this instance.
                InputSource is = new InputSource(systemId);
                is.setByteStream(in);
                reuse(xsr,is);
                return xsr;
            } catch (IllegalAccessException e) {
                throw new XMLReaderException("stax.cantCreate",e);
            } catch (InvocationTargetException e) {
                throw new XMLReaderException("stax.cantCreate",e);
            } catch (XMLStreamException e) {
                throw new XMLReaderException("stax.cantCreate",e);
            }
        }
        
        public XMLStreamReader doCreate(String systemId, Reader in, boolean rejectDTDs) {
            try {
                XMLStreamReader xsr = fetch(zephyrClass);
                if(xsr==null)
                    return xifMap.get(zephyrClass).createXMLStreamReader(systemId,in);
                
                // try re-using this instance.
                InputSource is = new InputSource(systemId);
                is.setCharacterStream(in);
                reuse(xsr,is);
                return xsr;
            } catch (IllegalAccessException e) {
                throw new XMLReaderException("stax.cantCreate",e);
            } catch (InvocationTargetException e) {
                throw new XMLReaderException("stax.cantCreate",e);
            } catch (XMLStreamException e) {
                throw new XMLReaderException("stax.cantCreate",e);
            }
        }
        
        /**
         * Fetchs an instance from the pool if available, otherwise null.
         */
        private XMLStreamReader fetch(Class<? extends XMLStreamReader> clazz) {
            Map<Class<?>, XMLStreamReader> implMap = getPoolMap();
            return implMap.remove(clazz);
        }
        
        private void offer(XMLStreamReader reader) {
            Map<Class<?>, XMLStreamReader> implMap = getPoolMap();
            implMap.put(reader.getClass(), reader);
        }
        
        private Map<Class<?>, XMLStreamReader> getPoolMap() {
            Map<Class<?>, XMLStreamReader> implMap = pool.get();
            if (implMap == null) {
                implMap = new HashMap<Class<?>, XMLStreamReader>(4);
                pool.set(implMap);
            }
            
            return implMap;
        }
        
        private void reuse(XMLStreamReader xsr, InputSource in) throws IllegalAccessException, InvocationTargetException {
            resetMethod.invoke(xsr);
            setInputSourceMethod.invoke(xsr,in);
        }
        
    }
    
    /**
     * Default {@link XMLStreamReaderFactory} implementation
     * that can work with any {@link XMLInputFactory}.
     *
     * <p>
     * {@link XMLInputFactory} is not required to be thread-safe, so the
     * create method on this implementation is synchronized.
     */
    public static final class Default extends NoLock {
        /**
         * Creates Default instance
         */
        public static @Nullable
                XMLStreamReaderFactory newInstance(XMLInputFactory xif) {
            try {
                Class<?> clazz = xif.createXMLStreamReader(new StringReader("<foo/>")).getClass();
                return new Default(xif, clazz);
            } catch (XMLStreamException e) {
                e.printStackTrace();
                return null;    // impossible to fail to parse <foo/>, but anyway
            }
        }
        
        public Default(XMLInputFactory xif, Class<?> clazz) {
            super(xif, clazz);
        }
        
        public synchronized XMLStreamReader doCreate(String systemId, InputStream in, boolean rejectDTDs) {
            return super.doCreate(systemId, in, rejectDTDs);
        }
        
        public synchronized XMLStreamReader doCreate(String systemId, Reader in, boolean rejectDTDs) {
            return super.doCreate(systemId, in, rejectDTDs);
        }
        
        public synchronized XMLStreamReader doCreate(String systemId, InputStream in, boolean rejectDTDs, Class<? extends XMLStreamReader> clazz) {
            return super.doCreate(systemId, in, rejectDTDs, clazz);
        }
    }
    
    /**
     * Similar to {@link Default} but doesn't do any synchronization.
     *
     * <p>
     * This is useful when you know your {@link XMLInputFactory} is thread-safe by itself.
     */
    public static class NoLock extends XMLStreamReaderFactory {
        private final Map<Class, XMLInputFactory> xifMap;
        
        /**
         * Default XMLStreamReader implementation class
         */
        private final Class defaultReaderClass;
        
        public NoLock(XMLInputFactory xif, Class<?> clazz) {
            defaultReaderClass = clazz;
            xifMap = new HashMap<Class, XMLInputFactory>(4);
            xifMap.put(defaultReaderClass, xif);
        }
        
        public void registerXMLInputFactory(Class<? extends XMLStreamReader> clazz, XMLInputFactory factory) {
            xifMap.put(clazz, factory);
        }
        
        public XMLStreamReader doCreate(String systemId, InputStream in, boolean rejectDTDs) {
            return doCreate(systemId, in, rejectDTDs, defaultReaderClass);
        }
        
        public XMLStreamReader doCreate(String systemId, InputStream in, boolean rejectDTDs, Class<? extends XMLStreamReader> clazz) {
            try {
                return xifMap.get(clazz).createXMLStreamReader(systemId,in);
            } catch (XMLStreamException e) {
                throw new XMLReaderException("stax.cantCreate",e);
            }
        }
        
        public XMLStreamReader doCreate(String systemId, Reader in, boolean rejectDTDs) {
            try {
                return xifMap.get(defaultReaderClass).createXMLStreamReader(systemId,in);
            } catch (XMLStreamException e) {
                throw new XMLReaderException("stax.cantCreate",e);
            }
        }
        
        public void doRecycle(XMLStreamReader r) {
            // there's no way to recycle with the default StAX API.
        }
    }
}
