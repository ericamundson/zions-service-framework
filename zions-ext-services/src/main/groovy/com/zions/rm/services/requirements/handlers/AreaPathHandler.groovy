package com.zions.rm.services.requirements.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AreaPathHandler extends RmBaseAttributeHandler {
	@Autowired
	@Value('${tfs.areapath}')
	String tfsAreaPath
	
	@Override
	public String getFieldName() {
		// TODO Auto-generated method stub
		return null
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		// TODO Auto-generated method stub
		return tfsAreaPath
	}

}
