package com.zions.rm.services.requirements.handlers

import org.springframework.stereotype.Component

@Component
class ReleaseHandler extends RmBaseAttributeHandler {

	@Override
	public String getFieldName() {
		
		return 'Release'
	}

	@Override
	public Object formatValue(Object val, Object itemData) {
		
		if (itemData.getID() == '1570117') {
			def i = 0
		}
		if (val == 'Not Assigned') {
			return null
		}
		else {
			StringJoiner joiner = new StringJoiner(';')
			def valArray = val.split(';',0)
			valArray.each { sval ->
				if (sval == 'Consumer Lending' || sval == 'One DOT One' || sval == 'One DOT Two - Not Used' ) {
					joiner.add('R1')
				}
				else if (sval == 'Commercial & Construction Lending' || sval == 'Two DOT One' || sval == 'Two DOT Two - Not Used' ||
						sval == 'Two DOT Five' || sval == 'Two DOT Nine') {
					joiner.add('R2')
				}
				else if (sval == 'Deposits') {
					joiner.add('R3')
				}
				else if (sval == 'Roadmapped') {
					joiner.add('Roadmapped')
				}
			}
			return val = joiner.toString()
		}
	}
}