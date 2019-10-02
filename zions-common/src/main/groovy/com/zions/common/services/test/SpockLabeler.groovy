package com.zions.common.services.test

trait SpockLabeler {
	def g_(String message = null) {
		if (message) {
			println "given: ${message}"
		}
		true
	}
	def a_(String message = null) {
		if (message) {
			println "and: ${message}"
		}
		true
	}
	def w_(String message = null) {
		if (message) {
			println "when: ${message}"
		}
		true
	}
	def t_(String message = null) {
		if (message) {
			println "then: ${message}"
		} else {
			println "then: No exceptions"
		}
		true
	}
	def s_(String message = null) {
		if (message) {
			println "setup: ${message}"
		}
		true
	}
	def c_(String message = null) {
		if (message) {
			println "cleanup: ${message}"
		} else {
			println "cleanup: resources"
		}
		true
	}
}


