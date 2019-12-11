package com.zions.qm.services.test.handlers

import org.apache.commons.lang.StringEscapeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.ICacheManagementService
import com.zions.qm.services.test.ClmTestManagementService
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil

@Component('QmStepsHandler')
@Slf4j
class StepsHandler extends QmBaseAttributeHandler {
	@Autowired
	ClmTestManagementService clmTestManagementService

	@Autowired
	IAttachments attachmentService

	@Autowired
	ICacheManagementService cacheManagementService

	@Autowired
	@Value('clm.url')
	String clmUrl

	def SPECIAL = ['&nbsp;', '&copy;', '&reg;', '&euro;', '&trade;', '&larr;', '&uarr;', '&rarr;', '&darr;', '&ldquo;', '&rdquo;', '&lsquo;', '&rsquo;', '&ndash;']

	public String getQmFieldName() {

		return 'none'
	}

	public def formatValue(def value, def data) {
		def itemData = data.itemData
		def id = data.id
		String outVal = null
		def ts = getTestScript(itemData)
		if (ts != null) {
			outVal = buildStepData(ts, id)
		}
		return outVal;
	}

	String buildStepData(ts, String id) {
		def teststeps = ts.steps.step

		if (teststeps.size()> 0) {
			def writer = new StringWriter()
			MarkupBuilder stepxml = new MarkupBuilder(new IndentPrinter(new PrintWriter(writer), "", false))
			int sCount = 2
			int count = 0
			stepxml.steps(steps: 0, last: teststeps.size()+1) {
				teststeps.each { astep ->
					count++
					if (astep.link.size() > 0) {
						String refUrl = "${astep.link.@href}"
						def refData = getRefSteps(refUrl)
						def subSteps = refData.script
						def keyword = refData.keyword
						if (keyword) {
							String kwTitle = "${keyword.title.text()}"
							String kwWebId = "${keyword.webId.text()}"
							if (!subSteps) {
								String tsName = "${ts.title.text()}"
								String webId = "${ts.webId.text()}"
								//StepsHandler.log.warn("No test script for linked test step (${count}) of test script::  ${webId}: ${tsName}, Keyword:: ${kwWebId}: ${kwTitle}")
								step(id: sCount, type:'ValidateStep') {
									String html = StringEscapeUtils.escapeHtml("<DIV><P><b>${kwTitle}</b></P></DIV>")
									parameterizedString(isformatted: 'true') {
										mkp.yieldUnescaped html
									}
									html = StringEscapeUtils.escapeHtml('<DIV><P></P></DIV>')
									parameterizedString(isformatted: 'true') {
										mkp.yieldUnescaped html
									}
								}
								sCount++
							} else {
								subSteps.steps.step.each { sstep ->
									step(id: sCount, type:'ValidateStep') {
										if (sstep.description.div.size() > 0) {
											String htmlDoc = XmlUtil.serialize( sstep.description.div )
											//CharMatcher m
											//htmlDoc = htmlDoc.replaceAll("\\p{Cntrl}", "")
											htmlDoc = htmlDoc.replace('tag0:', '')
											htmlDoc = htmlDoc.replace(' xmlns:tag0="http://www.w3.org/1999/xhtml"', '')
											htmlDoc = htmlDoc.replace(' dir="ltr"', '')
											//htmlDoc = processImages(htmlDoc, id)
											//htmlDoc = removeHyperlinks(htmlDoc)
											htmlDoc = htmlDoc.replace('<?xml version="1.0" encoding="UTF-8"?>', '')
											//htmlDoc = htmlDoc.replaceAll(/&+(\w)+;/,'')
											//htmlDoc = htmlDoc.replace('&copy;','')
											//String plainText= Jsoup.parse(htmlDoc).text();
											//							if (htmlDoc.length() == 0) {
											//								htmlDoc = 'n/a'
											//							}
											htmlDoc = "<DIV><P><b>${kwTitle}</b></P><P>${htmlDoc}</P></DIV>"
											//							File action = new File('action.html')
											//							def os = action.newDataOutputStream()
											//							os << htmlDoc
											//							os.close()
											//htmlDoc = htmlDoc.replace('&amp;', '&')
											//htmlDoc = StringEscapeUtils.unescapeHtml(htmlDoc)
											String html = escapeHtml(htmlDoc)
											parameterizedString(isformatted: 'true') {
												mkp.yieldUnescaped html
											}
										} else {
											String html = StringEscapeUtils.escapeHtml('<DIV><P></P></DIV>')
											parameterizedString(isformatted: 'true') {
												mkp.yieldUnescaped html
											}
										}
										if (sstep.expectedResult.div.size() > 0) {
											String htmlDoc = XmlUtil.serialize( sstep.expectedResult.div )
											//htmlDoc = htmlDoc.replaceAll("\\p{Cntrl}", "")
											htmlDoc = htmlDoc.replace('tag0:', '')
											htmlDoc = htmlDoc.replace(' xmlns:tag0="http://www.w3.org/1999/xhtml"', '')
											htmlDoc = htmlDoc.replace(' dir="ltr"', '')
											//							htmlDoc = htmlDoc.replaceAll(/&+(\w)+;/,'')
											//htmlDoc = processImages(htmlDoc, id)
											//htmlDoc = removeHyperlinks(htmlDoc)
											htmlDoc = htmlDoc.replace('<?xml version="1.0" encoding="UTF-8"?>', '')
											//String plainText= Jsoup.parse(htmlDoc).text();
											//							if (htmlDoc.length() == 0) {
											//								htmlDoc = 'n/a'
											//							}
											htmlDoc = "<DIV><P>${htmlDoc}</P></DIV>"
											//							File action = new File('result.html')
											//							def os = action.newDataOutputStream()
											//							os << htmlDoc
											//							os.close()
											//htmlDoc = StringEscapeUtils.unescapeHtml(htmlDoc)
											String html = escapeHtml(htmlDoc)
											parameterizedString(isformatted: 'true') {
												mkp.yieldUnescaped html
											}
										} else {
											String html = StringEscapeUtils.escapeHtml('<DIV><P></P></DIV>')
											parameterizedString(isformatted: 'true') {
												mkp.yieldUnescaped html
											}

										}
										//description()
									}
									sCount++
								}
							}
						}
					} else {
						step(id: sCount, type:'ValidateStep') {
							if (astep.description.div.size() > 0) {
								String htmlDoc = XmlUtil.serialize( astep.description.div )
								//CharMatcher m
								//htmlDoc = htmlDoc.replaceAll("\\p{Cntrl}", "")
								htmlDoc = htmlDoc.replace('tag0:', '')
								htmlDoc = htmlDoc.replace(' xmlns:tag0="http://www.w3.org/1999/xhtml"', '')
								htmlDoc = htmlDoc.replace(' dir="ltr"', '')
								//htmlDoc = processImages(htmlDoc, id)
								//htmlDoc = removeHyperlinks(htmlDoc)
								htmlDoc = htmlDoc.replace('<?xml version="1.0" encoding="UTF-8"?>', '')
								//htmlDoc = htmlDoc.replaceAll(/&+(\w)+;/,'')
								//htmlDoc = htmlDoc.replace('&copy;','')
								//String plainText= Jsoup.parse(htmlDoc).text();
								//							if (htmlDoc.length() == 0) {
								//								htmlDoc = 'n/a'
								//							}
								htmlDoc = "<DIV><P>${htmlDoc}</P></DIV>"
								//							File action = new File('action.html')
								//							def os = action.newDataOutputStream()
								//							os << htmlDoc
								//							os.close()
								//htmlDoc = htmlDoc.replace('&amp;', '&')
								//htmlDoc = StringEscapeUtils.unescapeHtml(htmlDoc)
								String html = escapeHtml(htmlDoc)
								parameterizedString(isformatted: 'true') {
									mkp.yieldUnescaped html
								}
							} else {
								String html = StringEscapeUtils.escapeHtml('<DIV><P></P></DIV>')
								parameterizedString(isformatted: 'true') {
									mkp.yieldUnescaped html
								}
							}
							if (astep.expectedResult.div.size() > 0) {
								String htmlDoc = XmlUtil.serialize( astep.expectedResult.div )
								//htmlDoc = htmlDoc.replaceAll("\\p{Cntrl}", "")
								htmlDoc = htmlDoc.replace('tag0:', '')
								htmlDoc = htmlDoc.replace(' xmlns:tag0="http://www.w3.org/1999/xhtml"', '')
								htmlDoc = htmlDoc.replace(' dir="ltr"', '')
								//							htmlDoc = htmlDoc.replaceAll(/&+(\w)+;/,'')
								//htmlDoc = processImages(htmlDoc, id)
								//htmlDoc = removeHyperlinks(htmlDoc)
								htmlDoc = htmlDoc.replace('<?xml version="1.0" encoding="UTF-8"?>', '')
								//String plainText= Jsoup.parse(htmlDoc).text();
								//							if (htmlDoc.length() == 0) {
								//								htmlDoc = 'n/a'
								//							}
								htmlDoc = "<DIV><P>${htmlDoc}</P></DIV>"
								//							File action = new File('result.html')
								//							def os = action.newDataOutputStream()
								//							os << htmlDoc
								//							os.close()
								//htmlDoc = StringEscapeUtils.unescapeHtml(htmlDoc)
								String html = escapeHtml(htmlDoc)
								parameterizedString(isformatted: 'true') {
									mkp.yieldUnescaped html
								}
							} else {
								String html = StringEscapeUtils.escapeHtml('<DIV><P></P></DIV>')
								parameterizedString(isformatted: 'true') {
									mkp.yieldUnescaped html
								}

							}
							//description()
						}
						sCount++
					}
				}
			}
			String outVal = writer.toString()
			outVal = outVal.replaceAll("\\p{Cntrl}", "")
			outVal = clearSpecial(outVal)
			//			outVal = outVal.replace('&nbsp;','')
			//			outVal = outVal.replace('&copy;','')
			//outVal = outVal.replaceAll("\\s","")
			return outVal
		}
		return null
	}

	def getRefSteps(String url) {
		def retVal = [:]
		def keyword = clmTestManagementService.getTestItem(url)
		if (keyword) {
			retVal['keyword'] = keyword
		}
		if (keyword && keyword.testscript.size() > 0) {
			String scriptUrl = "${keyword.testscript.@href}"
			def script = clmTestManagementService.getTestItem(scriptUrl)
			retVal['script'] = script
			return retVal
		}
		return retVal
	}

	String escapeHtml(String html) {
		String ohtml = html
		//ohtml = ohtml.replace('&', '&amp;')
		ohtml = ohtml.replace('<', '&lt;')
		ohtml = ohtml.replace('>', '&gt;')
		return ohtml
	}

	def clearSpecial(String val) {
		String out = val
		SPECIAL.each { sym ->
			out = out.replace(sym,'')
		}
		return out
	}

	private def getTestScript(def itemData) {
		def tss = itemData.testscript
		if (tss.size() > 0) {
			String href = "${tss[0].@href}"
			def ts = clmTestManagementService.getTestItem(href)
			return ts
		}
		return null
	}

	String removeHyperlinks(String html) {
		def htmlData = new XmlSlurper().parseText(html)
		def links = htmlData.'**'.findAll { p ->
			String href = p.@href
			"${href}".startsWith('http')
		}
		links.each { node ->
			String href = node.@href
			node.replaceNode {
				b("${href}")
			}
		}
		String outHtml = XmlUtil.asString(htmlData)
		return outHtml


	}

	String processImages(String html, String sId) {
		def htmlData = new XmlSlurper().parseText(html)
		def imgs = htmlData.'**'.findAll { p ->
			String src = p.@src
			"${p.name()}" == 'img' && "${src}".startsWith(this.clmUrl)
		}
		imgs.each { img ->
			String url = img.@src
			def oData = clmTestManagementService.getContent(url)
			String fName = cleanTextContent(oData.filename)
			def file = cacheManagementService.saveBinaryAsAttachment(oData.data, fName, sId)
			def attData = attachmentService.sendAttachment([file:file])
			img.@src = attData.url
		}
		String outHtml = XmlUtil.asString(htmlData)
		return outHtml
	}
	
	private static String cleanTextContent(String text)
	{
		if (text.lastIndexOf('\\') > -1) {
			text = text.substring(text.lastIndexOf('\\')+1)
		}
		// strips off all non-ASCII characters
		text = text.replaceAll("[^\\x00-\\x7F]", "");
 
		// erases all the ASCII control characters
		text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
		 
		// removes non-printable characters from Unicode
		text = text.replaceAll("\\p{C}", "");
 
		return text.trim();
	}



}
