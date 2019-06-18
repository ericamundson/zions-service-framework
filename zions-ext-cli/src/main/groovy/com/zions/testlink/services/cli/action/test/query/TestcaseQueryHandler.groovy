package com.zions.testlink.services.cli.action.test.query

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.IQueryHandler
import com.zions.testlink.services.test.TestLinkClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

import br.eti.kinoshita.testlinkjavaapi.constants.TestCaseDetails
import br.eti.kinoshita.testlinkjavaapi.model.TestCase
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan
import br.eti.kinoshita.testlinkjavaapi.model.TestProject
import br.eti.kinoshita.testlinkjavaapi.model.TestSuite
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor


class TestcaseQueryHandler implements IQueryHandler {
	
	@Autowired
	TestLinkClient testLinkClient
	
	@Autowired(required=false)
	ICacheManagementService cacheManagementService

	@Autowired
	IGenericRestClient qmGenericRestClient


	@Value('${testlink.projectName:}')
	String projectName
	
	@Value('${item.filter:qmAllFilter}')
	private String itemFilter

	def currentItems
	
	int page = 0
	
	Date currentTimestamp = new Date()
	
	TestSuite[] testSuites
	
	public def getItems() {
		if (!testSuites) {
			TestProject project = testLinkClient.getTestProjectByName(projectName)
			testSuites = testLinkClient.getFirstLevelTestSuitesForTestProject(project.id)
		}
		if (testSuites.length > 0) {
			TestCase[] tc = testLinkClient.getTestCasesForTestSuite(testSuites[0].id, true, TestCaseDetails.FULL)
			return tc
		}
		return null
	}

	public String initialUrl() {
		if (testSuites.length > 0) {
			return testSuites[0].name
		}
		return null
	}

	public String getPageUrl() {
		if (testSuites.length > 0) {
			return testSuites[page+1].name
		}
		return null
	}

	public Object nextPage() {
		page++
		if (testSuites.length > 0 && page < testSuites.length) {
			TestCase[] tc = testLinkClient.getTestCasesForTestSuite(testSuites[page].id, true, TestCaseDetails.FULL)
			return tc
		}
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
