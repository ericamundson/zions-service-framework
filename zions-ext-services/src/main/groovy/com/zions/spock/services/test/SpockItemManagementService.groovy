package com.zions.spock.services.test

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component


@Component
class SpockItemManagementService {
	@Value('${ado.area.path:}')
	String areaPath
	
	@Value('${ado.iteration.path')
	String iterationPath
	
	@Value('${tfs.project}')
	String project
	

}
