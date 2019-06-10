package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.ICacheManagementService
import com.zions.rm.services.requirements.ClmArtifact
import com.zions.rm.services.requirements.ClmRequirementsFileManagementService
import com.zions.rm.services.requirements.ClmRequirementsManagementService
import com.zions.rm.services.requirements.RequirementsMappingManagementService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil

@Component
@Slf4j
class DescriptionHandler extends RmBaseAttributeHandler {

	@Autowired
	ClmRequirementsManagementService clmRequirementsManagementService
	
	@Autowired
	IAttachments attachmentService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	RequirementsMappingManagementService rmMappingManagementService
	
	@Autowired	
	ClmRequirementsFileManagementService rmFileManagementService
	
	@Autowired
	@Value('${clm.url}')
	String clmUrl
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return 'Primary Text'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {		
		String sId = itemData.getCacheID()
		String outHtml
		if (itemData.getArtifactType() == 'Report Filter') {
			String seq = itemData.getAttribute('Sequence No')
			String filterDesc = itemData.getAttribute('Filter Description')
			outHtml = """<div><p><b>Sequence No:</b>&nbsp$seq</p>
                         <p><b>Filter Description:</b>&nbsp$filterDesc</p>"""
		}
		else if (itemData.getArtifactType() == 'Report Group Layout') {
			String section = itemData.getAttribute('Sections')
			String alignment = itemData.getAttribute('Alignment')
			String pageBreak = itemData.getAttribute('Page Break')
			outHtml = """<div><p><b>Section:</b>&nbsp$section</p>
                         <p><b>Alignment:</b>&nbsp$alignment</p>
                         <p><b>Page Break:</b>&nbsp$pageBreak</p>
                         <p><b>Value:</b>&nbsp${itemData.stripTags(value)}</p>"""
		}		
		else if (itemData.getArtifactType() == 'Report Sort') {
			String seq = itemData.getAttribute('Sequence No')
			String groupClause = itemData.getAttribute('Group Clause')
			String fieldName = itemData.getAttribute('Field Name')
			String gsb = itemData.getAttribute('Group or Sort')
			String order = itemData.getAttribute('Sort Order')
			String remarks = itemData.getAttribute('Info Comments')
			outHtml = """<div><p><b>Sequence No:</b>&nbsp$seq</p>
                         <p><b>Group Clause:</b>&nbsp$groupClause</p>
                         <p><b>Report Field Name:</b>&nbsp$fieldName</p>
                         <p><b>Group / Sort / Break:</b>&nbsp$gsb</p>
                         <p><b>Sort Order:</b>&nbsp$order</p>
                         <p><b>Remarks: </b>&nbsp$remarks</p></div>"""
		}
		else if (itemData.getArtifactType() == 'Report Summary Fields') {
			String field = itemData.getAttribute('Field Name')
			String valFormat = itemData.getAttribute('Value Format')
			String alignment = itemData.getAttribute('Alignment')
			String calc = itemData.getAttribute('Calculation Needed - Y/N')
			outHtml = """<div><p><b>Report Field Name:</b>&nbsp$field</p>
                         <p><b>Value Format:</b>&nbsp$valFormat</p>
                         <p><b>Data Alignment:</b>&nbsp$alignment</p>
                         <p><b>Field Mapping and Calculation:</b>&nbsp$calc</p>"""
		}
		else if (itemData.getArtifactType() == 'Reporting RRZ') {
			String title = itemData.getAttribute('Report Title')
			String pageSize = itemData.getAttribute('Page Size')
			String pageLayout = itemData.getAttribute('Page Layout')
			String docName = itemData.getAttribute('Document Name')
			String docNum = itemData.getAttribute('FS Document #')
			String audience = itemData.getAttribute('Audience')
			String distSchedule = itemData.getAttribute('Distribution Schedule')
			String distFormat = itemData.getAttribute('Distribution Format')
			String distMethod = itemData.getAttribute('Non-CORE System')
			String desc = itemData.getAttribute('Info Comments')
			String affiliates = itemData.getAttribute('Affiliates Affected')
			outHtml = """<div><p><b>Report Title:</b><br>
                         &nbsp&nbsp$title</p>
                         <p><b>Business Requirements:</b><br>
                         &nbsp&nbsp$docName<br>
                         &nbsp&nbsp$docNum</p>
                         <p><b>Description:</b><br>
                         &nbsp&nbsp$desc</p>
                         <p><b>Audience:</b><br>
                         &nbsp&nbsp${audience.replaceAll(';',', ')}</p>
                         <p><b>Distribution:</b><br>
						 &nbsp&nbspDistribution Schedule:&nbsp$distSchedule<br>
						 &nbsp&nbspDistribution Format:&nbsp$distFormat<br>
						 &nbsp&nbspDistribution Method:&nbsp${distMethod.replaceAll(';',', ')}</p>
                         <p><b>General Report and Page Layout:</b><br>
						 &nbsp&nbspPage Size:&nbsp$pageSize<br>
						 &nbsp&nbspPage Orientation:&nbsp$pageLayout</p></div>
                         <p><b>Input Parameters:</b><br>
                         &nbsp&nbspAffiliate:&nbsp${affiliates.replaceAll(';',', ')}</p>"""
		}
		else if (itemData.getArtifactType() == 'Data Interface TCS') {
			String touchpoint = itemData.getTitle()
			String flowType = itemData.getAttribute('Flow Type')
			String baNCSDirectionTCS = itemData.getAttribute('BaNCS Direction TCS')
			String dataContent = itemData.getAttribute('Data Content')
			String flowLevel = itemData.getAttribute('Flow Level')
			String ufe = itemData.getAttribute('UFE')
			String iszDataType = itemData.getAttribute('ISZ Data Type')
			String format = itemData.getAttribute('Format')
			String versionofMessageFormat = itemData.getAttribute('Version of Message Format')
			String multipleVersions = itemData.getAttribute('Multiple Versions')
			String messageTypes = itemData.getAttribute('Message Types')
			String characterSet = itemData.getAttribute('Character Set')
			String frequencyofData = itemData.getAttribute('Frequency of Data')
			String channelProtocol = itemData.getAttribute('Channel Protocol')
			String channelDirection = itemData.getAttribute('Channel Direction')
			String channelFailover = itemData.getAttribute('Channel Failover/offline')
			String channelRetryLimit = itemData.getAttribute('Channel Retry Limit')
			String channelValidationofData = itemData.getAttribute('Channel Validation of Data')
			String channelRegistrationofData = itemData.getAttribute('Channel Registration of Data')
			String duplicateCheck = itemData.getAttribute('Duplicate Check')
			String duplicateCriteria = itemData.getAttribute('Duplicate Criteria')
			String domainValidation = itemData.getAttribute('Domain Validation')
			String networkValidation = itemData.getAttribute('Network Validation')
			String xsdValidation = itemData.getAttribute('XSD Validation')
			String groupingUngroupingofdatarequired = itemData.getAttribute('Grouping/Ungrouping of data required')
			String groupingUngroupingcriteria = itemData.getAttribute('Grouping/Ungrouping criteria')
			String businessMapping = itemData.getAttribute('Business Mapping')
			String staticRouting = itemData.getAttribute('Static Routing')
			String dynamicRouting = itemData.getAttribute('Dynamic Routing')
			String multipleRouting = itemData.getAttribute('Multiple Routing')
			String routingCriteria = itemData.getAttribute('Routing Criteria')
			String reconciliation = itemData.getAttribute('Reconciliation')
			String rejectFileonError = itemData.getAttribute('Reject File on Error')
			String noDataHandling = itemData.getAttribute('No Data Handling')
			String alertCriteria = itemData.getAttribute('Alert Criteria')
			String alertProcedure = itemData.getAttribute('Alert Procedure')
			String technicalExceptionHandling = itemData.getAttribute('Technical Exception Handling')
			String businessExceptionHandling = itemData.getAttribute('Business Exception Handling')
			String dependenciesonTransactions = itemData.getAttribute('Dependencies on Transactions')
			String dependenciesonEOD = itemData.getAttribute('Dependencies on EOD')
			String dependenciesonCutOffTime = itemData.getAttribute('Dependencies on Cut-Off Time')
			String dependenciesonApps = itemData.getAttribute('Dependencies on Apps')
			String dependencyOwner = itemData.getAttribute('Dependency Owner')
			String serviceIntegratorRequirement = itemData.getAttribute('Service Integrator Requirement')
			String archival = itemData.getAttribute('Archival/Retention Period')
			String purgingPeriod = itemData.getAttribute('Purging Period')
			String allMessageReport = itemData.getAttribute('All Message Report')
			String exceptionReport = itemData.getAttribute('Exception Report')
			String reconciliationReport = itemData.getAttribute('Reconciliation Report')
			String dataMigrationRequirement = itemData.getAttribute('Data Migration Requirement')
			String multiEntityImpact = itemData.getAttribute('Multi Entity Impact')
			String phasedImplementationImpact = itemData.getAttribute('Phased Implementation Impact')
			String tps = itemData.getAttribute('TPS')
			String payloadSize = itemData.getAttribute('Payload Size')
			String responseTime = itemData.getAttribute('Response Time')
			String subscriptionRate = itemData.getAttribute('Subscription Rate')
			String timetoSubscribe = itemData.getAttribute('Time to Subscribe')
			String executionTime = itemData.getAttribute('Execution Time')
			String recordCount = itemData.getAttribute('Record Count')
			String timeofDay = itemData.getAttribute('Time of Day')
			outHtml = """<div>
<table border="1" cellpadding="0" cellspacing="0" dir="ltr" id="_1559767314955" style="border-collapse:collapse;border:none; border-collapse : collapse; border-color : #696969; border-collapse : collapse; "><tbody><tr>
<td colspan="1" id="_1559767314967" rowspan="1" style="width:333px;border:solid black 1.0pt;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314778" style="margin-left:.25in;"><span style="color:white">Item</span></p></td>
<td colspan="1" id="_1559767314968" rowspan="1" style="width:333px;border:solid black 1.0pt;border-left:none;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314779" style="margin-left:.25in;"><span style="color:white">Value</span></p></td></tr> <tr><td colspan="1" id="_1559767314969" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314780" style="margin-left:.25in;"><b>Flow Type</b></p></td><td colspan="1" id="_1559767314970" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314781" style="margin-left:.25in;">$flowType</p></td></tr><tr><td colspan="1" id="_1559767314971" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314782" style="margin-left:.25in;"><b>Direction w.r.t. TCS BaNCS</b></p></td><td colspan="1" id="_1559767314972" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314783" style="margin-left:.25in;">$baNCSDirectionTCS</p> </td></tr><tr><td colspan="1" id="_1559767314973" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314784" style="margin-left:.25in;"><b>Data Content</b></p></td><td colspan="1" id="_1559767314974" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314785" style="margin-left:.25in;">$dataContent</p></td></tr><tr><td colspan="1" id="_1559767314975" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314786" style="margin-left:.25in;"><b>Flow level</b></p></td><td colspan="1" id="_1559767314976" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314787" style="margin-left:.25in;">$flowLevel</p></td></tr> <tr><td colspan="1" id="_1559767314977" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314788" style="margin-left:.25in;"><b>Part of UFE</b></p></td><td colspan="1" id="_1559767314978" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314789" style="margin-left:.25in;">$ufe</p></td></tr></tbody></table>
<h3 dir="ltr" id="_1559767314937">Data Specification</h3>
<p dir="ltr" id="_1559767314790" style="margin-left:.25in;">Specification ID: $touchpoint</p>
<p dir="ltr" id="_1559767314791" style="margin-left:.25in;">This section contains the information about the incoming or outgoing data including the data format.</p>
<table border="1" cellpadding="0" cellspacing="0" dir="ltr" id="_1559767314956" style="border-collapse:collapse;border:none; border-collapse : collapse; border-color : #696969; border-collapse : collapse; "><tbody><tr>
<td colspan="1" id="_1559767314979" rowspan="1" style="width:333px;border:solid black 1.0pt;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314792" style="margin-left:.25in;"><span style="color:white">Item</span></p></td>
<td colspan="1" id="_1559767314980" rowspan="1" style="width:333px;border:solid black 1.0pt;border-left:none;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314793" style="margin-left:.25in;"><span style="color:white">Value</span></p></td></tr> <tr><td colspan="1" id="_1559767314981" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314794" style="margin-left:.25in;"><b>Data Type</b></p></td>
<td colspan="1" id="_1559767314982" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314795" style="margin-left:.25in;">$iszDataType</p></td></tr><tr><td colspan="1" id="_1559767314983" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314796" style="margin-left:.25in;"><b>Data Type Format</b></p></td><td colspan="1" id="_1559767314984" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314797" style="margin-left:.25in;">$format</p></td></tr> <tr><td colspan="1" id="_1559767314985" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314798" style="margin-left:.25in;"><b>Version of the message format</b></p></td><td colspan="1" id="_1559767314986" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314799" style="margin-left:.25in;">$versionofMessageFormat</p></td></tr><tr><td colspan="1" id="_1559767314987" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314800" style="margin-left:.25in;"><b>Multiple Versions</b></p></td><td colspan="1" id="_1559767314988" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314801" style="margin-left:.25in;">$multipleVersions</p></td></tr><tr><td colspan="1" id="_1559767314989" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314802" style="margin-left:.25in;"><b>Message Types</b></p></td><td colspan="1" id="_1559767314990" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314803" style="margin-left:.25in;">$messageTypes</p></td></tr> <tr><td colspan="1" id="_1559767314991" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314804" style="margin-left:.25in;"><b>Character set</b></p></td> <td colspan="1" id="_1559767314992" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314805" style="margin-left:.25in;">$characterSet</p></td></tr><tr><td colspan="1" id="_1559767314993" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314806" style="margin-left:.25in;"><b>Frequency of Data</b></p></td><td colspan="1" id="_1559767314994" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314807" style="margin-left:.25in;">$frequencyofData</p> </td></tr></tbody></table>
<h3 dir="ltr" id="_1559767314938">Communication Channel</h3>
<p dir="ltr" id="_1559767314808" style="margin-left:.25in;">Specification ID: $touchpoint</p>
<p dir="ltr" id="_1559767314809" style="margin-left:.25in;">This section contains the information about the incoming or outgoing data including the data format.</p>
<table border="1" cellpadding="0" cellspacing="0" dir="ltr" id="_1559767314957" style="border-collapse:collapse;border:none; border-collapse : collapse; border-color : #696969; border-collapse : collapse; "><tbody><tr>
<td colspan="1" id="_1559767314995" rowspan="1" style="width:333px;border:solid black 1.0pt;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314810" style="margin-left:.25in;"><span style="color:white">Item</span></p></td>
<td colspan="1" id="_1559767314996" rowspan="1" style="width:333px;border:solid black 1.0pt;border-left:none;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314811" style="margin-left:.25in;"><span style="color:white">Value</span></p></td></tr> <tr><td colspan="1" id="_1559767314997" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314812" style="margin-left:.25in;"><b>Protocol</b></p></td><td colspan="1" id="_1559767314998" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314813" style="margin-left:.25in;">$channelProtocol</p></td></tr><tr><td colspan="1" id="_1559767314999" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314814" style="margin-left:.25in;"><b>Direction</b></p></td><td colspan="1" id="_1559767315000" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314815" style="margin-left:.25in;">$channelDirection</p></td></tr> <tr><td colspan="1" id="_1559767315001" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314816" style="margin-left:.25in;"><b>Failover / offline channel</b></p></td><td colspan="1" id="_1559767315002" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314817" style="margin-left:.25in;">$channelFailover</p></td></tr><tr><td colspan="1" id="_1559767315003" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314818" style="margin-left:.25in;"><b>Channel retry limit</b></p></td><td colspan="1" id="_1559767315004" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314819" style="margin-left:.25in;">$channelRetryLimit</p> </td></tr><tr><td colspan="1" id="_1559767315005" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314820" style="margin-left:.25in;"><b>Validation of data at channel required</b></p></td><td colspan="1" id="_1559767315006" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314821" style="margin-left:.25in;">$channelValidationofData</p></td></tr><tr><td colspan="1" id="_1559767315007" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314822" style="margin-left:.25in;"><b>Registration of the data required</b></p></td><td colspan="1" id="_1559767315008" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314823" style="margin-left:.25in;">$channelRegistrationofData</p></td></tr></tbody></table>
<h3 dir="ltr" id="_1559767314939">IO Data Format</h3>
<p dir="ltr" id="_1559767314824" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<p dir="ltr" id="_1559767314825" style="margin-left:.25in;">The following IOF document will be generated separately from Rational: ${touchpoint.replace("ISZ-", "IOF-")}.xlsx</p>
<h3 dir="ltr" id="_1559767314940">Processing</h3>
<p dir="ltr" id="_1559767314826" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<table border="1" cellpadding="0" cellspacing="0" dir="ltr" id="_1559767314958" style="border-collapse:collapse;border:none; border-collapse : collapse; border-color : #696969; border-collapse : collapse; "><tbody><tr>
<td colspan="1" id="_1559767315009" rowspan="1" style="width:333px;border:solid black 1.0pt;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314827" style="margin-left:.25in;"><span style="color:white">Item</span></p></td>
<td colspan="1" id="_1559767315010" rowspan="1" style="width:333px;border:solid black 1.0pt;border-left:none;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314828" style="margin-left:.25in;"><span style="color:white">Value</span></p></td></tr> <tr><td colspan="1" id="_1559767315011" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314829" style="margin-left:.25in;"><b>When is the data produced</b></p></td><td colspan="1" id="_1559767315012" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314830" style="margin-left:.25in;"><span style="color:black">$value</span></p></td></tr></tbody></table>
<h3 dir="ltr" id="_1559767314941">Validation</h3>
<p dir="ltr" id="_1559767314831" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<table border="1" cellpadding="0" cellspacing="0" dir="ltr" id="_1559767314959" style="border-collapse:collapse;border:none; border-collapse : collapse; border-color : #696969; border-collapse : collapse; "><tbody><tr>
<td colspan="1" id="_1559767315013" rowspan="1" style="width:333px;border:solid black 1.0pt;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314832" style="margin-left:.25in;"><span style="color:white">Item</span></p></td>
<td colspan="1" id="_1559767315014" rowspan="1" style="width:333px;border:solid black 1.0pt;border-left:none;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314833" style="margin-left:.25in;"><span style="color:white">Value</span></p></td></tr> <tr><td colspan="1" id="_1559767315015" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314834" style="margin-left:.25in;"><b>Duplicate check required</b></p></td><td colspan="1" id="_1559767315016" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314835" style="margin-left:.25in;">$duplicateCheck</p></td></tr><tr><td colspan="1" id="_1559767315017" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314836" style="margin-left:.25in;"><b>Duplicate criteria</b></p></td><td colspan="1" id="_1559767315018" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314837" style="margin-left:.25in;">$duplicateCriteria</p></td></tr> <tr><td colspan="1" id="_1559767315019" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314838" style="margin-left:.25in;"><b>Domain Validation</b></p> </td><td colspan="1" id="_1559767315020" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314839" style="margin-left:.25in;">$domainValidation</p></td></tr><tr><td colspan="1" id="_1559767315021" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314840" style="margin-left:.25in;"><b>Network Validation</b></p></td><td colspan="1" id="_1559767315022" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314841" style="margin-left:.25in;">$networkValidation</p> </td></tr><tr><td colspan="1" id="_1559767315023" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314842" style="margin-left:.25in;"><b>XSD Validation</b></p></td><td colspan="1" id="_1559767315024" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314843" style="margin-left:.25in;">$xsdValidation</p></td></tr></tbody></table>
<h3 dir="ltr" id="_1559767314942">Grouping/Ungrouping</h3>
<p dir="ltr" id="_1559767314844" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<table border="1" cellpadding="0" cellspacing="0" dir="ltr" id="_1559767314960" style="border-collapse:collapse;border:none; border-collapse : collapse; border-color : #696969; border-collapse : collapse; "><tbody><tr>
<td colspan="1" id="_1559767315025" rowspan="1" style="width:333px;border:solid black 1.0pt;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314845" style="margin-left:.25in;"><span style="color:white">Item</span></p></td>
<td colspan="1" id="_1559767315026" rowspan="1" style="width:333px;border:solid black 1.0pt;border-left:none;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314846" style="margin-left:.25in;"><span style="color:white">Value</span></p></td></tr> <tr><td colspan="1" id="_1559767315027" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314847" style="margin-left:.25in;"><b>Grouping/Ungrouping of data required</b></p></td><td colspan="1" id="_1559767315028" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314848" style="margin-left:.25in;">$groupingUngroupingofdatarequired</p></td></tr><tr><td colspan="1" id="_1559767315029" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314849" style="margin-left:.25in;"><b>Grouping/Ungrouping criteria</b></p></td><td colspan="1" id="_1559767315030" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314850" style="margin-left:.25in;">$groupingUngroupingcriteria</p></td></tr></tbody></table>
<h3 dir="ltr" id="_1559767314943">Business Mapping</h3>
<p dir="ltr" id="_1559767314851" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<p dir="ltr" id="_1559767314852" style="margin-left:.25in;">$businessMapping</p>
<h3 dir="ltr" id="_1559767314944">Routing</h3>
<p dir="ltr" id="_1559767314853" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<table border="1" cellpadding="0" cellspacing="0" dir="ltr" id="_1559767314961" style="border-collapse:collapse;border:none; border-collapse : collapse; border-color : #696969; border-collapse : collapse; "><tbody><tr>
<td colspan="1" id="_1559767315031" rowspan="1" style="width:333px;border:solid black 1.0pt;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314854" style="margin-left:.25in;"><span style="color:white">Item</span></p></td>
<td colspan="1" id="_1559767315032" rowspan="1" style="width:333px;border:solid black 1.0pt;border-left:none;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314855" style="margin-left:.25in;"><span style="color:white">Value</span></p></td></tr> <tr><td colspan="1" id="_1559767315033" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314856" style="margin-left:.25in;"><b>Static routing</b></p></td> <td colspan="1" id="_1559767315034" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314857" style="margin-left:.25in;">$staticRouting</p></td></tr><tr><td colspan="1" id="_1559767315035" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314858" style="margin-left:.25in;"><b>Dynamic routing</b></p></td><td colspan="1" id="_1559767315036" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314859" style="margin-left:.25in;">$dynamicRouting</p></td></tr> <tr><td colspan="1" id="_1559767315037" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314860" style="margin-left:.25in;"><b>Multiple routing</b></p></td> <td colspan="1" id="_1559767315038" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314861" style="margin-left:.25in;">$multipleRouting</p></td></tr><tr><td colspan="1" id="_1559767315039" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314862" style="margin-left:.25in;"><b>Routing criteria</b></p></td><td colspan="1" id="_1559767315040" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314863" style="margin-left:.25in;">$routingCriteria</p></td></tr> </tbody></table>
<h3 dir="ltr" id="_1559767314945">Reconciliation</h3>
<p dir="ltr" id="_1559767314864" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<p dir="ltr" id="_1559767314865" style="margin-left:.25in;">$reconciliation</p>
<h3 dir="ltr" id="_1559767314946">Exception Handling</h3>
<p dir="ltr" id="_1559767314866" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<table border="1" cellpadding="0" cellspacing="0" dir="ltr" id="_1559767314962" style="border-collapse:collapse;border:none; border-collapse : collapse; border-color : #696969; border-collapse : collapse; "><tbody><tr>
<td colspan="1" id="_1559767315041" rowspan="1" style="width:333px;border:solid black 1.0pt;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314867" style="margin-left:.25in;"><span style="color:white">Item</span></p></td>
<td colspan="1" id="_1559767315042" rowspan="1" style="width:333px;border:solid black 1.0pt;border-left:none;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314868" style="margin-left:.25in;"><span style="color:white">Value</span></p></td></tr> <tr><td colspan="1" id="_1559767315043" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314869" style="margin-left:.25in;"><b>Reject complete file when there is error in one record</b></p></td><td colspan="1" id="_1559767315044" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314870" style="margin-left:.25in;">$rejectFileonError</p></td></tr><tr><td colspan="1" id="_1559767315045" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314871" style="margin-left:.25in;"><b>No data condition handling</b></p></td><td colspan="1" id="_1559767315046" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314872" style="margin-left:.25in;">$noDataHandling</p></td></tr><tr><td colspan="1" id="_1559767315047" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314873" style="margin-left:.25in;"><b>Alert criteria</b></p></td><td colspan="1" id="_1559767315048" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314874" style="margin-left:.25in;">$alertCriteria</p></td></tr><tr><td colspan="1" id="_1559767315049" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314875" style="margin-left:.25in;"><b>Alert procecure</b></p></td><td colspan="1" id="_1559767315050" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314876" style="margin-left:.25in;">$alertProcedure</p></td></tr><tr><td colspan="1" id="_1559767315051" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314877" style="margin-left:.25in;"><b>Technical Exception Handling</b></p></td><td colspan="1" id="_1559767315052" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314878" style="margin-left:.25in;">$technicalExceptionHandling</p> </td></tr><tr><td colspan="1" id="_1559767315053" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314879" style="margin-left:.25in;"><b>Business Exception Handling</b></p></td><td colspan="1" id="_1559767315054" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314880" style="margin-left:.25in;">$businessExceptionHandling</p></td></tr></tbody></table>
<h3 dir="ltr" id="_1559767314947">Dependencies</h3>
<p dir="ltr" id="_1559767314881" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<table border="1" cellpadding="0" cellspacing="0" dir="ltr" id="_1559767314963" style="border-collapse:collapse;border:none; border-collapse : collapse; border-color : #696969; border-collapse : collapse; "><tbody><tr>
<td colspan="1" id="_1559767315055" rowspan="1" style="width:333px;border:solid black 1.0pt;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314882" style="margin-left:.25in;"><span style="color:white">Item</span></p></td><td colspan="1" id="_1559767315056" rowspan="1" style="width:333px;border:solid black 1.0pt;border-left:none;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314883" style="margin-left:.25in;"><span style="color:white">Value</span></p></td></tr> <tr><td colspan="1" id="_1559767315057" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314884" style="margin-left:.25in;"><b>Dependencies on any other online transaction</b></p></td><td colspan="1" id="_1559767315058" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314885" style="margin-left:.25in;">$dependenciesonTransactions</p></td></tr><tr><td colspan="1" id="_1559767315059" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314886" style="margin-left:.25in;"><b>Dependencies on EOD</b></p></td><td colspan="1" id="_1559767315060" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314887" style="margin-left:.25in;">$dependenciesonEOD</p></td></tr><tr><td colspan="1" id="_1559767315061" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314888" style="margin-left:.25in;"><b>Dependencies on cut-off time</b></p></td><td colspan="1" id="_1559767315062" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314889" style="margin-left:.25in;">$dependenciesonCutOffTime</p></td></tr><tr><td colspan="1" id="_1559767315063" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314890" style="margin-left:.25in;"><b>Dependencies on other applications</b></p></td><td colspan="1" id="_1559767315064" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314891" style="margin-left:.25in;">$dependenciesonApps</p></td></tr><tr><td colspan="1" id="_1559767315065" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314892" style="margin-left:.25in;"><b>Responsible party for managing the dependencies</b></p></td><td colspan="1" id="_1559767315066" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314893" style="margin-left:.25in;">$dependencyOwner</p></td></tr></tbody></table>
<h3 dir="ltr" id="_1559767314948">Service Integrator Requirements</h3>
<p dir="ltr" id="_1559767314894" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<p dir="ltr" id="_1559767314895" style="margin-left:.25in;">$serviceIntegratorRequirement</p>
<h3 dir="ltr" id="_1559767314949">Archival Requirements</h3>
<p dir="ltr" id="_1559767314896" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<table border="1" cellpadding="0" cellspacing="0" dir="ltr" id="_1559767314964" style="border-collapse:collapse;border:none; border-collapse : collapse; border-color : #696969; border-collapse : collapse; "><tbody><tr>
<td colspan="1" id="_1559767315067" rowspan="1" style="width:333px;border:solid black 1.0pt;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314897" style="margin-left:.25in;"><span style="color:white">Item</span></p></td><td colspan="1" id="_1559767315068" rowspan="1" style="width:333px;border:solid black 1.0pt;border-left:none;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314898" style="margin-left:.25in;"><span style="color:white">Value</span></p></td></tr> <tr><td colspan="1" id="_1559767315069" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314899" style="margin-left:.25in;"><b>Archival/Retention period</b></p></td><td colspan="1" id="_1559767315070" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314900" style="margin-left:.25in;">$archival</p></td></tr><tr><td colspan="1" id="_1559767315071" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314901" style="margin-left:.25in;"><b>Purge period</b></p></td><td colspan="1" id="_1559767315072" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314902" style="margin-left:.25in;">$purgingPeriod</p></td></tr> </tbody></table>
<h3 dir="ltr" id="_1559767314950">Reporting Requirements</h3>
<p dir="ltr" id="_1559767314903" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<table border="1" cellpadding="0" cellspacing="0" dir="ltr" id="_1559767314965" style="border-collapse:collapse;border:none; border-collapse : collapse; border-color : #696969; border-collapse : collapse; "><tbody><tr>
<td colspan="1" id="_1559767315073" rowspan="1" style="width:333px;border:solid black 1.0pt;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314904" style="margin-left:.25in;"><span style="color:white">Item</span></p></td><td colspan="1" id="_1559767315074" rowspan="1" style="width:333px;border:solid black 1.0pt;border-left:none;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314905" style="margin-left:.25in;"><span style="color:white">Value</span></p></td></tr> <tr><td colspan="1" id="_1559767315075" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314906" style="margin-left:.25in;"><b>All message Report</b></p> </td><td colspan="1" id="_1559767315076" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314907" style="margin-left:.25in;">$allMessageReport</p></td></tr><tr><td colspan="1" id="_1559767315077" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314908" style="margin-left:.25in;"><b>Exception Report</b></p></td><td colspan="1" id="_1559767315078" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314909" style="margin-left:.25in;">$exceptionReport</p></td></tr> <tr><td colspan="1" id="_1559767315079" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314910" style="margin-left:.25in;"><b>Reconciliation Report</b></p> </td><td colspan="1" id="_1559767315080" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314911" style="margin-left:.25in;">$reconciliationReport</p></td></tr></tbody></table>
<h3 dir="ltr" id="_1559767314951">Data Migration Requirements</h3>
<p dir="ltr" id="_1559767314912" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<p dir="ltr" id="_1559767314913" style="margin-left:.25in;">$dataMigrationRequirement</p>
<h3 dir="ltr" id="_1559767314952">Multi Entity Impact</h3>
<p dir="ltr" id="_1559767314914" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<p dir="ltr" id="_1559767314915" style="margin-left:.25in;">$multiEntityImpact</p>
<h3 dir="ltr" id="_1559767314953">Phased Implementation Approach Impact</h3>
<p dir="ltr" id="_1559767314916" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<p dir="ltr" id="_1559767314917" style="margin-left:.25in;">$phasedImplementationImpact</p>
<h3 dir="ltr" id="_1559767314954">Service Level Agreement (shared SLA – all parties must comply)</h3>
<p dir="ltr" id="_1559767314918" style="margin-left:.25in;">Specification ID: $touchpoint</p>
<table border="1" cellpadding="0" cellspacing="0" dir="ltr" id="_1559767314966" style="border-collapse:collapse;border:none; border-collapse : collapse; border-color : #696969; border-collapse : collapse; "><tbody><tr>
<td colspan="1" id="_1559767315081" rowspan="1" style="width:333px;border:solid black 1.0pt;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314919" style="margin-left:.25in;"><span style="color:white">Item</span></p></td><td colspan="1" id="_1559767315082" rowspan="1" style="width:333px;border:solid black 1.0pt;border-left:none;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314920" style="margin-left:.25in;"><span style="color:white">Value</span></p></td></tr> <tr><td colspan="1" id="_1559767315083" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314921" style="margin-left:.25in;"><b>TPS (near real-time or online)</b></p></td><td colspan="1" id="_1559767315084" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314922" style="margin-left:.25in;">$tps</p></td></tr><tr><td colspan="1" id="_1559767315085" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314923" style="margin-left:.25in;"><b>Payload size (for batch this is row size)</b></p></td><td colspan="1" id="_1559767315086" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314924" style="margin-left:.25in;">$payloadSize</p></td></tr><tr><td colspan="1" id="_1559767315087" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314925" style="margin-left:.25in;"><b>Response time (online)</b></p></td><td colspan="1" id="_1559767315088" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314926" style="margin-left:.25in;">$responseTime</p></td></tr><tr><td colspan="1" id="_1559767315089" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314927" style="margin-left:.25in;"><b>Subscription rate (near real-time)</b></p></td><td colspan="1" id="_1559767315090" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314928" style="margin-left:.25in;">$subscriptionRate</p></td></tr><tr><td colspan="1" id="_1559767315091" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314929" style="margin-left:.25in;"><b>Time to subscribe (near real-time)</b></p></td><td colspan="1" id="_1559767315092" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314930" style="margin-left:.25in;">$timetoSubscribe</p></td></tr><tr><td colspan="1" id="_1559767315093" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314931" style="margin-left:.25in;"><b>Execution time (batch)</b></p></td><td colspan="1" id="_1559767315094" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314932" style="margin-left:.25in;">$executionTime</p></td></tr> <tr><td colspan="1" id="_1559767315095" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314933" style="margin-left:.25in;"><b>Record count (batch)</b></p> </td><td colspan="1" id="_1559767315096" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314934" style="margin-left:.25in;">$recordCount</p></td></tr><tr><td colspan="1" id="_1559767315097" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314935" style="margin-left:.25in;"><b>Time of day (batch)</b></p></td><td colspan="1" id="_1559767315098" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314936" style="margin-left:.25in;">$timeofDay</p></td> </tr></tbody></table>
</div>"""
		}
		else if (itemData.getFormat() == 'WrapperResource') {
			// For wrapper resource (uploaded file), we need to create our own description with hyperlink to attachment
			def fileItem = rmFileManagementService.ensureRequirementFileAttachment(itemData, itemData.getFileHref())
			if (fileItem) {
				if (isImageFile("${fileItem.fileName}")) {
					// convert uploaded image to embedded image
					outHtml = "<div><img alt='Embedded image' src='" + fileItem.url + "'/></div>"
				}
				else {
					// include link to attached document
					outHtml = "<div><a href='" + fileItem.url + "&amp;download=true'>Uploaded Attachment: ${fileItem.fileName}</a></div>"
				}
			} else {
				outHtml = "<div>Uploading Attachment from CLM failed, please see original work item</div>"
			}
		}
		else if (value == null || value.length() == 0) {
			return '<div></div>'
		}		
		else {
			// strip out all namespace stuff from html
			String description = "${value}".replace("h:div xmlns:h='http://www.w3.org/1999/xhtml'",'div').replace('<h:','<').replace('</h:','</')
			description = description.replace('div xmlns="http://www.w3.org/1999/xhtml"','div')

			// Process any embedded images and table formatting
			outHtml = processHtml(description, sId, itemData)
		}
		outHtml = outHtml.replaceAll("&lt;",'<').replaceAll("&gt;",'>').replaceAll("[^\\p{ASCII}]", "")

		return outHtml
	}
	
	def processHtml(String html, String sId, def itemData) {
		def htmlData
		try {
			htmlData = new XmlSlurper().parseText(html)
		}
		catch (Exception e) {
			log.error("Error parsing description for ID $sId: ${e.getMessage()}")
			return null
		}
		// First move all embedded images or embedded attachments to ADO
		def wrapperRootNode
		def imgs = htmlData.'**'.findAll { p ->
			String src = p.@src
			"${p.name()}" == 'img' && "${src}".startsWith(this.clmUrl)
		}
		imgs.each { img ->
			String url = img.@src
			def fileItem
			// If the embedded image was due to an embedded wrapper resource artifact, we want to get the original document attachment
			int wrapNdx = url.indexOf('resourceRevisionURL')
			if (wrapNdx > 0) {
				// Need to pull in the attachment for the embedded wrapped resource
				def about = clmUrl + '/rm/resources/' + url.substring(wrapNdx+74)
				def wrappedResourceArtifact = new ClmArtifact('','',about)
				wrappedResourceArtifact = clmRequirementsManagementService.getNonTextArtifact(wrappedResourceArtifact, false)
				fileItem = rmFileManagementService.ensureRequirementFileAttachment(itemData, wrappedResourceArtifact.getFileHref())
				
				// Now delete image node
				String attachmentLink
				if (isImageFile("${fileItem.fileName}")) {
					// Convert uploaded image to an embedded image
					attachmentLink = "<div><img alt='Embedded image' src='" + fileItem.url + "'/></div>"
				}
				else {
					attachmentLink = "<div><a href='" + fileItem.url + "&amp;download=true'>Uploaded Attachment: ${fileItem.fileName}</a></div>"
				}
				if (wrapperRootNode) {
					wrapperRootNode.appendNode(new XmlSlurper().parseText(attachmentLink))
				}
				else {
					wrapperRootNode = new XmlSlurper().parseText(attachmentLink)
				}
			}
			else {
				fileItem = rmFileManagementService.ensureRequirementFileAttachment(itemData, url)
				if(fileItem) {
				img.@src = fileItem.url		
				} else {
					log.error("Error uploading attachment for ID ${sId}")
				}
			}
			

		}
		// If there are any embedded documents, we just return the wrapper document links
		if (wrapperRootNode) {
			htmlData = wrapperRootNode
		}	
		else {	
			// Next process all tables, adding border info to <td> tags
			addBorderStyle('th', htmlData)
			addBorderStyle('td', htmlData)
		}

		// Return html as string, but remove <?xml tag as it causes issues
		return XmlUtil.asString(htmlData).replace('<?xml version="1.0" encoding="UTF-8"?>\n', '')

	}
	
	def addBorderStyle(String tag, def htmlData) {
		def tds = htmlData.'**'.findAll { p ->
			"${p.name()}" == "${tag}"
		}
		tds.each { td ->
			String style = td.@style
			td.@style = style + ';border:1px solid black'
		}		
	}

	boolean isImageFile(String filename) {
		return (filename.toLowerCase().indexOf('.png') > 0 || filename.toLowerCase().indexOf('.jpg') > 0)
	}
}
