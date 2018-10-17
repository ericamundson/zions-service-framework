package com.zions.qm.services.metadata

import com.zions.qm.services.rest.QmGenericRestClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class QmMetadataManagementService {
	
	@Autowired
	QmGenericRestClient qmGenericRestClient
	
	public QmMetadataManagementService() {
		
	}

	def extractQmMetadata(String projectArea, File templateDir) {
		
	}
}
