package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class NameHandler extends RmBaseAttributeHandler {
	static int SIZE = 255
	
	@Override
	public String getFieldName() {
		
		return 'title'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		String outVal = null
		if (itemData.getArtifactType() == 'Assumption' ||
			itemData.getArtifactType() == 'Report Mockup' ||
			itemData.getArtifactType() == 'Report Fields' ||
			itemData.getArtifactType() == 'Report Filter' || 
			itemData.getArtifactType() == 'Report Group Layout' || 
			itemData.getArtifactType() == 'Report Summary Fields') {
			outVal = stripFullPrefix("${value}")
		}	
		else if (itemData.getArtifactType() == 'Report Sort') {
			outVal = stripPrefix("${value}")
		}
		else if (value != null && value != '') {
			outVal = "${value}"
		}
		else {
			outVal = '<blank title>'
		}
		// Truncate if too long
		if (outVal.length() > SIZE) {
			outVal = outVal.substring(0, SIZE-1)
		}
		return outVal;
	}
	private String stripFullPrefix(String value) {
		def ndx = value.lastIndexOf(': ')
		if (ndx > 0) {
			return value.substring(ndx+2)
		}
		else {
			return value
		}
	}
	private String stripPrefix(String value) {
		def ndx = value.indexOf(': ')
		if (ndx > 0) {
			return value.substring(ndx+2)
		}
		else {
			return value
		}
	}
}
