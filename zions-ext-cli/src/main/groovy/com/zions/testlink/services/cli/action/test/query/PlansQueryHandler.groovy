package com.zions.testlink.services.cli.action.test.query

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.IQueryHandler
import com.zions.testlink.services.test.TestLinkClient
import br.eti.kinoshita.testlinkjavaapi.model.TestCase
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan
import br.eti.kinoshita.testlinkjavaapi.model.TestProject

@Component
class PlansQueryHandler implements IQueryHandler {
	@Autowired
	TestLinkClient testLinkClient
	
	@Autowired(required=false)
	ICacheManagementService cacheManagementService

	@Value('${testlink.projectName:}')
	String projectName
	
	@Value('${plans.filter:tlAllFilter}')
	private String itemFilter

	def currentItems
	
	int page = 0
	
	Date currentTimestamp = new Date()
	
	TestPlan[] testPlans
	
	public def getItems() {
		if (!testPlans) {
			TestProject project = testLinkClient.getTestProjectByName(projectName)
			testPlans = testLinkClient.getProjectTestPlans(project.id)
		}
		if (testPlans.length > 0) {
			return testPlans
		}
		return null
	}

	public String initialUrl() {
		if (testPlans.length > 0) {
			return testPlans[0].name
		}
		return null
	}

	public String getPageUrl() {
		return null
	}

	public Object nextPage() {
		return null
	}

	
	public String getFilterName() {
		return this.itemFilter;
	}
	

	public boolean isModified(Object item) {
		String key = "${item.id}"
		def cacheWI = cacheManagementService.getFromCache(key, ICacheManagementService.WI_DATA)
		if (!cacheWI) return true
		String savedVersion = cacheWI.fields['Custom.ExternalRev']
		String iVersion = "${item.version}"
		if (iVersion != savedVersion) return true
		return false;
	}

}
