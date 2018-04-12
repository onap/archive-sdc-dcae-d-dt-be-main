package org.onap.sdc.dcae.checker.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;


/** */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD}) 
public @interface Validates {
  String rule() default "/";
	String[] timing() default { "post" };
}
