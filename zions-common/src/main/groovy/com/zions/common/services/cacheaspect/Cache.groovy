package com.zions.common.services.cacheaspect

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target


@Target([ElementType.METHOD])
@Retention(RetentionPolicy.RUNTIME)
@interface Cache {
	Class elementType()
}

