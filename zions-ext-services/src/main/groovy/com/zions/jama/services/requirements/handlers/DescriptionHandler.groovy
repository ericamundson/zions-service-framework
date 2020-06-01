package com.zions.jama.services.requirements.handlers

import org.springframework.stereotype.Component
import com.zions.rm.services.requirements.ClmArtifact
import com.zions.jama.services.requirements.JamaRequirementsFileManagementService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil

@Component
@Slf4j
class DescriptionHandler extends RmBaseAttributeHandler {
	@Autowired	
	JamaRequirementsFileManagementService jamaFileManagementService

	@Value('${jama.url}')
	String jamaUrl

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

		// Return html as string, but remove <?xml tag as it causes issues
		return XmlUtil.asString(htmlData)

	}

	boolean isImageFile(String filename) {
		return (filename.toLowerCase().indexOf('.png') > -1 || filename.toLowerCase().indexOf('.jpg') > -1 || 
				filename.toLowerCase().indexOf('.jpeg') > -1 || (filename.indexOf('.') == -1 && filename.indexOf('Image') > -1))
	}
}
