package com.zions.auto.base

class CompletedSteps {
	def completedSteps = []
	def startTime = (new Date().getTime())
	long elapsedSec = 0

	void add(stepName) {
		elapsedSec = ((new Date().getTime()) - startTime) / 1000
		completedSteps.add(stepName + " (Elapsed seconds: $elapsedSec)")
		println(stepName)
	}
	public String formatForHtml() {
		String html = '<br><p>Completed Steps:<br><ol>'
		completedSteps.forEach { step ->
			html = html + '<li>' + step + '</li>'
		}
		html = html + '</ol></p>'
	}
	public String formatForLog() {
		String str = 'Completed Steps:\n'
		completedSteps.forEach { step ->
			str = str + '* ' + step + '\n'
		}
		return str
	}
}
