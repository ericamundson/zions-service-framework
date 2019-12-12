package com.zions.common.services.rest

class ThrottleException extends Exception {
	ThrottleException(String msg) {
		super(msg)
	}
}
