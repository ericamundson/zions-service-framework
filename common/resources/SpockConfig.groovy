
report {
	enabled true
	logFileDir './build/spock'
	logFileName 'spock-report.json'
	//logFileSuffix new Date().format('yyyy-MM-dd_HH_mm_ss')
}
spockReports {
	set 'com.athaydes.spockframework.report.showCodeBlocks': true
	set 'com.athaydes.spockframework.report.outputDir': 'build/spock-reports'
	set 'com.athaydes.spockframework.report.internal.HtmlReportCreator.enabled': false
	set 'com.athaydes.spockframework.report.template.TemplateReportCreator.enabled': true
	set 'com.athaydes.spockframework.report.IReportCreator': 'com.athaydes.spockframework.report.template.TemplateReportCreator'
	
	// Set properties of the report creator
	set 'com.athaydes.spockframework.report.template.TemplateReportCreator.specTemplateFile': '/templateReportCreator/my-spec-template.xml'
	set 'com.athaydes.spockframework.report.template.TemplateReportCreator.reportFileExtension': 'spock.xml'
	set 'com.athaydes.spockframework.report.template.TemplateReportCreator.summaryTemplateFile': '/templateReportCreator/my-summary-template.md'
	set 'com.athaydes.spockframework.report.template.TemplateReportCreator.summaryFileName': 'summary.md'
	
	// Output directory (where the spock reports will be created) - relative to working directory
	//set 'com.athaydes.spockframework.report.outputDir': 'build/spock-reports'
	
	 //If set to true, hides blocks which do not have any description
	//set 'com.athaydes.spockframework.report.hideEmptyBlocks': false
}