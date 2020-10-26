package com.zions.jama.services.requirements.handlers

import org.springframework.stereotype.Component
import com.zions.rm.services.requirements.ClmArtifact
import com.zions.jama.services.requirements.JamaRequirementsFileManagementService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.NodeChild
import groovy.xml.XmlUtil
import groovy.xml.StreamingMarkupBuilder

@Component
@Slf4j
class DescriptionHandler extends RmBaseAttributeHandler {
	@Autowired	
	JamaRequirementsFileManagementService jamaFileManagementService

	@Value('${jama.url}')
	String jamaUrl

	def DescriptionHandler() {
		// Add method to NodeChild to support conversion to html
		NodeChild.metaClass.toXmlString = {
			def self = delegate
			new StreamingMarkupBuilder().bind {
//				delegate.mkp.xmlDeclaration() // Use this if you want an XML declaration
				delegate.out << self
			}.toString()
		}

	}
	@Override
	public String getFieldName() {
		
		return 'description'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {		
		if (value == null || value == '') {
			return toHtml(value)
		} else if (value.indexOf('<img alt') > -1) {
			String sId = itemData.getCacheID()
			return processHtml(value, sId, itemData)
		} else {
			return value
		}
	}
	
	def processHtml(String html, String sId, def itemData) {
		def htmlData
		try {
			html = '<div>' + html.replace('&ldquo;','"').replace('&rdquo;','"').replace('&nbsp;', ' ').replace('&ndash;','-').replace('&mdash;','-').replace('&rsquo;',"'").replace('&lsquo;',"'").replace('&hellip;',"...").replace('&bull;',"*").replace('&middot;',"-").replace('&copy;',"(Copyright)").replace('<strong><description< span=\"\"></description<></strong>','') + '</div>'
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
			"${p.name()}" == 'img' && "${src}".startsWith(this.jamaUrl)
		}
		imgs.each { img ->
			String url = "${img.@src}"
			def fileItem = jamaFileManagementService.ensureRequirementFileAttachment(itemData, url)
			if(fileItem) {
				img.@src = fileItem.url		
			} else {
				log.error("Error uploading attachment for ID ${sId}")
			}
			

		}

		// Return html as string
		def sHtml = htmlData.toXmlString()
		return sHtml

	}

	boolean isImageFile(String filename) {
		return (filename.toLowerCase().indexOf('.svg') > -1 || filename.toLowerCase().indexOf('.jpg') > -1 || 
				filename.toLowerCase().indexOf('.jpeg') > -1 || (filename.indexOf('.') == -1 && filename.indexOf('Image') > -1))
	}
}
