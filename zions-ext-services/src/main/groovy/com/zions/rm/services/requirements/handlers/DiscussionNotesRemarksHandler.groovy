package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class DiscussionNotesRemarksHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'Discussion Notes'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		String formatValue
		// First check to see if there are any remarks from zions
		String remarks = itemData.getAttribute("Remarks")
		if (remarks.length()> 0) {
			formatValue = '<b>Zions:</b> ' + remarks
		}
		if (val != "") {
			if ( formatValue != "") {
				formatValue = formatValue + '<br><br>'
			}
			formatValue = formatValue + '<b>TCS:</b> ' + val
		}
		return toHtml(formatValue)
	}

}
