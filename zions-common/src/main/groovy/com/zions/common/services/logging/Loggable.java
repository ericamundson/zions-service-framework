package com.zions.common.services.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* Author: Michael Angelastro
 * Date: 3/27/2019
 */

/*Logging annotation to be used at class level
 * Loggable annotation for all methods of a class annotated with the @Loggable annotation*/

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {}