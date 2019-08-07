package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class DiscussionNotesRemarksIofHandler extends RmBaseAttributeHandler {
	
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
			formatValue = '<b>Remarks:</b> ' + remarks
		}
		if (val != "") {
			if ( formatValue != "") {
				formatValue = formatValue + '<br><br>'
			}
			formatValue = formatValue + '<b>Discussion Notes:</b> ' + val
		}
		return toHtml(formatValue)
	}

}
