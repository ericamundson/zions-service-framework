package com.zions.qm.services.test.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.zions.qm.services.test.ClmTestManagementService
import groovy.xml.MarkupBuilder

@Component
class PreconditionHandler extends HtmlHandler {

	public String getQmFieldName() {
		// TODO Auto-generated method stub
		return 'com.ibm.rqm.planning.editor.section.testCasePreCondition'
	}

	

}
