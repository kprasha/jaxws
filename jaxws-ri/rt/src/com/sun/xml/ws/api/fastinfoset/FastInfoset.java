package com.sun.xml.ws.api.fastinfoset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.xml.ws.spi.WebServiceFeatureAnnotation;

import com.sun.xml.ws.api.fastinfoset.FastInfosetFeature;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@WebServiceFeatureAnnotation(bean=FastInfosetFeature.class, id=FastInfosetFeature.ID)
public @interface FastInfoset {
	boolean enabled() default true;
}
