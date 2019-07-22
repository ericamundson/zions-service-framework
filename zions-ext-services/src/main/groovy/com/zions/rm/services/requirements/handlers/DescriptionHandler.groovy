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
		
		return 'Primary Text'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {		
		String sId = itemData.getCacheID()
		String outHtml
		if (itemData.getArtifactType() == 'Report Filter') {
			String seq = itemData.getTypeSeqNo()
			String filterDesc = itemData.getAttribute('Filter Description')
			outHtml = """<div><p style="margin-left: 40px">
<b>Sequence No:</b>&nbsp$seq<br>
<b>Filter Description:</b>&nbsp$filterDesc</p></div>"""
		}
		else if (itemData.getArtifactType() == 'Report Group Layout') {
			String section = itemData.getAttribute('Sections')
			String alignment = itemData.getAttribute('Alignment')
			String pageBreak = itemData.getAttribute('Page Break')
			outHtml = """<div><p style="margin-left: 40px">
<b>Section:</b>&nbsp$section<br>
<b>Alignment:</b>&nbsp$alignment<br>
<b>Page Break:</b>&nbsp$pageBreak<br>
<b>Value:</b>&nbsp${itemData.stripTags(value)}</p></div>"""
		}		
		else if (itemData.getArtifactType() == 'Report Sort') {
			String seq = itemData.getTypeSeqNo()
			String groupClause = itemData.getAttribute('Group Clause')
			String fieldName = itemData.getAttribute('Field Name')
			String gsb = itemData.getAttribute('Group or Sort')
			String order = itemData.getAttribute('Sort Order')
			String remarks = itemData.getAttribute('Info Comments')
			outHtml = """<div><p style="margin-left: 40px">
<b>Sequence No:</b>&nbsp$seq<br>
<b>Group Clause:</b>&nbsp$groupClause<br>
<b>Report Field Name:</b>&nbsp$fieldName<br>
<b>Group / Sort / Break:</b>&nbsp$gsb<br>
<b>Sort Order:</b>&nbsp$order<br>
<b>Remarks: </b>&nbsp$remarks</p></div>"""
		}
		else if (itemData.getArtifactType() == 'Report Summary Fields') {
			String field = itemData.getAttribute('Field Name')
			String valFormat = itemData.getAttribute('Value Format')
			String alignment = itemData.getAttribute('Data Alignment')
			String calc = value
			outHtml = """<div><p style="margin-left: 40px">
<b>Report Field Name:</b>&nbsp$field<br>
<b>Value Format:</b>&nbsp$valFormat<br>
<b>Data Alignment:</b>&nbsp$alignment<br>
<b>Field Mapping and Calculation:</b>&nbsp$calc</p></div>"""
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
			outHtml = """<div><p style="margin-left: 40px">
<b>Report Title:</b><br>
$title<br>
<b>Business Requirements:</b><br>
$docName<br>
$docNum<br>
<b>Description:</b><br>
$desc<br>
<b>Audience:</b><br>
${audience.replaceAll(';',', ')}<br>
<b>Distribution:</b><br>
Distribution Schedule:&nbsp$distSchedule<br>
Distribution Format:&nbsp$distFormat<br>
Distribution Method:&nbsp${distMethod.replaceAll(';',', ')}<br>
<b>General Report and Page Layout:</b><br>
Page Size:&nbsp$pageSize<br>
Page Orientation:&nbsp$pageLayout<br>
<b>Input Parameters:</b><br>
Affiliate:&nbsp${affiliates.replaceAll(';',', ')}</p></div>"""
		}
		else if (itemData.getArtifactType() == 'Data Interface TCS') {
			value = removeNamespace("${value}")
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
			String dependenciesonTransactions = itemData.getAttribute('Dependencies on Transactions')
			String dependenciesonEOD = itemData.getAttribute('Dependencies on EOD')
			String dependenciesonCutOffTime = itemData.getAttribute('Dependencies on Cut-Off Time')
			String dependenciesonApps = itemData.getAttribute('Dependencies on Apps')
			String dependencyOwner = itemData.getAttribute('Dependency Owner')
			String allMessageReport = itemData.getAttribute('All Message Report')
			String exceptionReport = itemData.getAttribute('Exception Report')
			String reconciliationReport = itemData.getAttribute('Reconciliation Report')
			String dataMigrationRequirement = itemData.getAttribute('Data Migration Requirement')
			String multiEntityImpact = itemData.getAttribute('Multi Entity Impact')
			String phasedImplementationImpact = itemData.getAttribute('Phased Implementation Impact')
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
<h3 dir="ltr" id="_1559767314940">Processing</h3>
<p dir="ltr" id="_1559767314826" style="margin-left:.25in;">Specification ID:$touchpoint</p>
<table border="1" cellpadding="0" cellspacing="0" dir="ltr" id="_1559767314958" style="border-collapse:collapse;border:none; border-collapse : collapse; border-color : #696969; border-collapse : collapse; "><tbody><tr>
<td colspan="1" id="_1559767315009" rowspan="1" style="width:333px;border:solid black 1.0pt;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314827" style="margin-left:.25in;"><span style="color:white">Item</span></p></td>
<td colspan="1" id="_1559767315010" rowspan="1" style="width:333px;border:solid black 1.0pt;border-left:none;background:#0080FF;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314828" style="margin-left:.25in;"><span style="color:white">Value</span></p></td></tr> <tr><td colspan="1" id="_1559767315011" rowspan="1" style="width:333px;border:solid black 1.0pt;border-top:none;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314829" style="margin-left:.25in;"><b>When is the data produced</b></p></td><td colspan="1" id="_1559767315012" rowspan="1" style="width:333px;border-top:none;border-left:none;border-bottom:solid black 1.0pt;border-right:solid black 1.0pt;padding:0in 5.4pt 0in 5.4pt;vertical-align:top; border-color : #696969; ">
<p id="_1559767314830" style="margin-left:.25in;"><span style="color:black">$value</span></p></td></tr></tbody></table>
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
			String description = removeNamespace("${value}")

			// Process any embedded images and table formatting
			outHtml = processHtml(description, sId, itemData)
		}
		outHtml = outHtml.replaceAll("&lt;",'<').replaceAll("&gt;",'>').replaceAll("[^\\p{ASCII}]", "")

		return outHtml
	}
	String removeNamespace(String value) {
		String description = value.replace("h:div xmlns:h='http://www.w3.org/1999/xhtml'",'div').replace('<h:','<').replace('</h:','</')
		description = description.replace('div xmlns="http://www.w3.org/1999/xhtml"','div')
		return description
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
				wrappedResourceArtifact = clmRequirementsManagementService.getNonTextArtifact(wrappedResourceArtifact, false, false)
				fileItem = rmFileManagementService.ensureRequirementFileAttachment(itemData, wrappedResourceArtifact.getFileHref())
				if(fileItem) {
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
				} else {
					log.error("unable to parse a file attachment of ID ${sId}")
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
		return (filename.toLowerCase().indexOf('.png') > -1 || filename.toLowerCase().indexOf('.jpg') > -1 || 
				filename.toLowerCase().indexOf('.jpeg') > -1 || (filename.indexOf('.') == -1 && filename.indexOf('Image') > -1))
	}
}
