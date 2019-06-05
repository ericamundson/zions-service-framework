package com.zions.qm.services.test.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.qm.services.test.ClmTestManagementService
import groovy.xml.MarkupBuilder

@Component('QmPostconditionHandler')
class PostconditionHandler extends HtmlHandler {

	public String getQmFieldName() {
		
		return 'com.ibm.rqm.planning.editor.section.testCasePostCondition'
	}

	

}
