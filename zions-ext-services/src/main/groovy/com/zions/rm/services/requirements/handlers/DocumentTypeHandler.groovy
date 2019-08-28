package com.zions.rm.services.requirements.handlers

import com.zions.rm.services.requirements.ClmRequirementsModule
import org.springframework.stereotype.Component

@Component
class DocumentTypeHandler extends RmBaseAttributeHandler {
	
	@Override
	public String getFieldName() {
		
		return 'Artifact Type'
	}

	@Override
	public Object formatValue(Object value, Object itemData) {
		String outVal
		if (value == 'RSZ Specification') {
			ClmRequirementsModule module 
			module = itemData
			if (itemData.appendedDocumentType == 'Reporting RRZ') {
				outVal = 'Reporting RSZ'
			}
			else if (itemData.appendedDocumentType == 'Statements and Notices RRZ Spec') {
				outVal = 'SnF RSZ'
			}
			else {
				outVal = "$value"
			}
		}
		else if (value == 'Statements and Notices RRZ Spec') {
			outVal = 'SnF RRZ'
		}
		else {
			outVal = "$value"
		}
		return outVal;
	}

}
