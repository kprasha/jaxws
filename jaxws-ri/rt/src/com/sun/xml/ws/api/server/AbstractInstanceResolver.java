package com.sun.xml.ws.api.server;

import com.sun.istack.Nullable;
import com.sun.xml.ws.resources.ServerMessages;
import com.sun.xml.ws.server.ServerRtException;
import com.sun.xml.ws.util.localization.Localizable;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Partial implementation of {@link InstanceResolver} with
 * convenience methods to do the resource injection.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractInstanceResolver<T> extends InstanceResolver<T> {

    /**
     * Encapsulates which field/method the injection is done,
     * and performs the injection.
     */
    protected static interface InjectionPlan<T> {
        void inject(T instance,WebServiceContext wsc);
    }

    /**
     * Injects to a field.
     */
    private static class FieldInjectionPlan<T> implements InjectionPlan<T> {
        private final Field field;

        public FieldInjectionPlan(Field field) {
            this.field = field;
        }

        public void inject(final T instance, final WebServiceContext wsc) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    try {
                        if (!field.isAccessible()) {
                            field.setAccessible(true);
                        }
                        field.set(instance,wsc);
                        return null;
                    } catch (IllegalAccessException e) {
                        throw new ServerRtException("server.rt.err",e);
                    }
                }
            });
        }
    }

    /**
     * Injects to a method.
     */
    private static class MethodInjectionPlan<T> implements InjectionPlan<T> {
        private final Method method;

        public MethodInjectionPlan(Method method) {
            this.method = method;
        }

        public void inject(final T instance, final WebServiceContext wsc) {
            invokeMethod(method, instance, wsc);
        }
    }

    /**
     * Combines multiple {@link InjectionPlan}s into one.
     */
    private static class Compositor<T> implements InjectionPlan<T> {
        private final InjectionPlan<T>[] children;

        public Compositor(Collection<InjectionPlan<T>> children) {
            this.children = children.toArray(new InjectionPlan[children.size()]);
        }

        public void inject(T instance, WebServiceContext wsc) {
            for (InjectionPlan<T> plan : children)
                plan.inject(instance,wsc);
        }
    }

    /**
     * Helper for invoking a method with elevated privilege.
     */
    protected static void invokeMethod(final @Nullable Method method, final Object instance, final Object... args) {
        if(method==null)    return;
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    method.invoke(instance,args);
                } catch (IllegalAccessException e) {
                    throw new ServerRtException("server.rt.err",e);
                } catch (InvocationTargetException e) {
                    throw new ServerRtException("server.rt.err",e);
                }
                return null;
            }
        });
    }

    /**
     * Finds the method that has the given annotation, while making sure that
     * there's only at most one such method.
     */
    protected final @Nullable Method findAnnotatedMethod(Class clazz, Class<? extends Annotation> annType) {
        boolean once = false;
        Method r = null;
        for(Method method : clazz.getDeclaredMethods()) {
            if (method.getAnnotation(annType) != null) {
                if (once) {
                    // Err: Multiple methods have annType annotation
                    throw new ServerRtException("annotation.only.once", annType );
                }
                if (method.getParameterTypes().length != 0) {
                    throw new ServerRtException("not.zero.parameters",
                        method.getName());
                }
                r = method;
                once = true;
            }
        }
        return r;
    }

    /**
     * Creates an {@link InjectionPlan} that injects {@link WebServiceContext} to the given class.
     */
    protected final InjectionPlan<T> buildInjectionPlan(Class clazz) {
        List<InjectionPlan<T>> plan = new ArrayList<InjectionPlan<T>>();

        for(Field field: clazz.getDeclaredFields()) {
            Resource resource = field.getAnnotation(Resource.class);
            if (resource != null) {
                if(isWebServiceContextInjectionPoint(resource, field.getType(),
                    ServerMessages.localizableWRONG_FIELD_TYPE(field.getName()))) {
                    plan.add(new FieldInjectionPlan<T>(field));
                }
            }
        }

        for(Method method : clazz.getDeclaredMethods()) {
            Resource resource = method.getAnnotation(Resource.class);
            if (resource != null) {
                Class[] paramTypes = method.getParameterTypes();
                if (paramTypes.length != 1) {
                    throw new ServerRtException("wrong.no.parameters",
                        method.getName());
                }
                if(isWebServiceContextInjectionPoint(resource,paramTypes[0],
                    ServerMessages.localizableWRONG_PARAMETER_TYPE(method.getName()))) {
                    plan.add(new MethodInjectionPlan<T>(method));
                }
            }
        }

        return new Compositor<T>(plan);
    }

    /**
     * Returns true if the combination of {@link Resource} and the field/method type
     * are consistent for {@link WebServiceContext} injection.
     */
    private boolean isWebServiceContextInjectionPoint(Resource resource, Class fieldType, Localizable errorMessage ) {
        Class resourceType = resource.type();
        if (resourceType.equals(Object.class)) {
            return fieldType.equals(WebServiceContext.class);
        } else if (resourceType.equals(WebServiceContext.class)) {
            if (fieldType.isAssignableFrom(WebServiceContext.class)) {
                return true;
            } else {
                throw new ServerRtException(errorMessage);
            }
        }
        return false;
    }
}
