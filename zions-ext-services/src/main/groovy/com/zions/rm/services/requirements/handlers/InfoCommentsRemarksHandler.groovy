package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class InfoCommentsRemarksHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'Info Comments'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		String formatValue
		// First check to see if there are any remarks from zions
		String remarks = itemData.getAttribute("Remarks")
		if (remarks && remarks != "") {
			formatValue = remarks
		}
		if (val && val != "") {
			if ( formatValue != "") {
				formatValue = formatValue + '<br><br>'
			}
			formatValue = formatValue + val
		}
		return toHtml(formatValue)
	}

}
