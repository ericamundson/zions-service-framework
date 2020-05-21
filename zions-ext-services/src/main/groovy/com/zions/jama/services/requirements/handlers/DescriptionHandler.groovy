package com.zions.jama.services.requirements.handlers

import org.springframework.stereotype.Component
import com.zions.rm.services.requirements.ClmArtifact

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil

@Component
@Slf4j
class DescriptionHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'description'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {		
		if (value == null || value == '') {
			return toHtml(value)
		} else {
			return value
		}
	}
}
