package org.onap.sdc.dcae.checker.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;


/** */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD}) 
/* The iffy part: as a type annotaton we do not need a path or a version specification,
   as a method annotation it is mandatory (cannot be the default) 
	 We could forsee that a version indcation at type level would cover all check handler within the type
 */
public @interface Checks {
  String path() default "/";
	String[] version() default { "1.0", "1.0.0", "1.1", "1.1.0" };
}
