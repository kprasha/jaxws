package com.sun.xml.ws.api.server;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.server.ServerRtException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

/**
 * {@link InstanceResolver} that always returns a single instance.
 *
 * @author Kohsuke Kawaguchi
 */
final class SingletonResolver<T> extends InstanceResolver<T> {
    private final T singleton;

    public SingletonResolver(T singleton) {
        this.singleton = singleton;
    }

    public T resolve(Packet request) {
        return singleton;
    }

    public void start(WebServiceContext wsc) {
        doFieldsInjection(wsc);
        doMethodsInjection(wsc);
        // notify that we are ready to serve
        invokeAnnotatedMethod(PostConstruct.class);
    }

    public void dispose() {
        invokeAnnotatedMethod(PreDestroy.class);
    }

    /**
     * Helper method to invoke a method that has the specified annotation.
     */
    private void invokeAnnotatedMethod(Class<? extends Annotation> annType) {
        Class c = singleton.getClass();
        boolean once = false;
        for(Method method : c.getDeclaredMethods()) {
            if (method.getAnnotation(annType) != null) {
                if (once) {
                    // Err: Multiple methods have annType annotation
                    throw new ServerRtException("annotation.only.once", annType );
                }
                if (method.getParameterTypes().length != 0) {
                    throw new ServerRtException("not.zero.parameters",
                        method.getName());
                }
                invokeMethod(method);
                once = true;
            }
        }
    }

    /**
     * Helper method to invoke a Method.
     */
    private void invokeMethod(final Method method, final Object... args) {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws IllegalAccessException, InvocationTargetException {
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    method.invoke(singleton, args);
                    return null;
                }
            });
        } catch(PrivilegedActionException e) {
            throw new ServerRtException("server.rt.err",e.getException());
        }
    }

    private void doFieldsInjection(WebServiceContext wsc) {
        Class c = singleton.getClass();
        Field[] fields = c.getDeclaredFields();
        for(final Field field: fields) {
            Resource resource = field.getAnnotation(Resource.class);
            if (resource != null) {
                Class resourceType = resource.type();
                Class fieldType = field.getType();
                if (resourceType.equals(Object.class)) {
                    if (fieldType.equals(javax.xml.ws.WebServiceContext.class)) {
                        injectField(field,wsc);
                    }
                } else if (resourceType.equals(javax.xml.ws.WebServiceContext.class)) {
                    if (fieldType.isAssignableFrom(javax.xml.ws.WebServiceContext.class)) {
                        injectField(field,wsc);
                    } else {
                        throw new ServerRtException("wrong.field.type",
                            field.getName());
                    }
                }
            }
        }
    }

    /*
     * injects a resource into a Field
     */
    private void injectField(final Field field,final WebServiceContext wsc) {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws IllegalAccessException {
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    field.set(singleton,wsc);
                    return null;
                }
            });
        } catch(PrivilegedActionException e) {
            throw new ServerRtException("server.rt.err",e.getException());
        }
    }

    private void doMethodsInjection(WebServiceContext wsc) {
        Class c = singleton.getClass();
        Method[] methods = c.getDeclaredMethods();
        for(final Method method : methods) {
            Resource resource = method.getAnnotation(Resource.class);
            if (resource != null) {
                Class[] paramTypes = method.getParameterTypes();
                if (paramTypes.length != 1) {
                    throw new ServerRtException("wrong.no.parameters",
                        method.getName());
                }
                Class resourceType = resource.type();
                Class argType = paramTypes[0];
                if (resourceType.equals(Object.class)
                    && argType.equals(javax.xml.ws.WebServiceContext.class)) {
                    invokeMethod(method,wsc);
                } else if (resourceType.equals(javax.xml.ws.WebServiceContext.class)) {
                    if (argType.isAssignableFrom(javax.xml.ws.WebServiceContext.class)) {
                        invokeMethod(method,wsc);
                    } else {
                        throw new ServerRtException("wrong.parameter.type",
                            method.getName());
                    }
                }
            }
        }
    }
}
