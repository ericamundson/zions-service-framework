import spock.lang.Specification

class LabelPrinter {
  def g_(String message) {
    print "given: ${message}"
    true
  }
  def a_(String message) {
    print "and: a_ ${message}"
    true
  }
  def w_(String message) {
    print "when: w_ ${message}"
    true
  }
  def t_(String message) {
    print "then: t_ ${message}"
    true
  }
  def s_(String message) {
    print "setup: ${message}"
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
