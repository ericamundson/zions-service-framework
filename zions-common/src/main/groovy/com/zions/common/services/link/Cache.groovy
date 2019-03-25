package com.zions.common.services.link

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

enum Module { CCM, QM, RM }

@Target([ElementType.METHOD])
@Retention(RetentionPolicy.RUNTIME)
@interface Cache {
	Module module()
}
