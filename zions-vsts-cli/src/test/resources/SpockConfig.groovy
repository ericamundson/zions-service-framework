import spock.lang.Specification

class LabelPrinter {
	def g_(String message = null) {
		if (message) {
			print "given: ${message}"
		}
		true
	}
	def a_(String message = null) {
		if (message) {
			print "and: ${message}"
		}
		true
	}
	def w_(String message = null) {
		if (message) {
			print "when: ${message}"
		}
		true
	}
	def t_(String message = null) {
		if (message) {
			print "then: ${message}"
		} else {
			print "then: No exceptions"
		}
		true
	}
	def s_(String message = null) {
		if (message) {
			print "setup: ${message}"
		}
		true
	}
	def c_(String message = null) {
		if (message) {
			print "cleanup: ${message}"
		} else {
			print "cleanup: resources"
		}
		true
	}
}

Specification.mixin LabelPrinter

report {
	enabled true
	logFileDir './build/spock'
	logFileName 'spock-report.json'
	//logFileSuffix new Date().format('yyyy-MM-dd_HH_mm_ss')
}
