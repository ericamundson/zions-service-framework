package com.zions.testlink.services.cli.action.test.query

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.IQueryHandler
import com.zions.testlink.services.test.TestLinkClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import br.eti.kinoshita.testlinkjavaapi.model.TestCase
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan
import br.eti.kinoshita.testlinkjavaapi.model.TestProject
import br.eti.kinoshita.testlinkjavaapi.model.TestSuite
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor

@Component
class TestcaseQueryHandler implements IQueryHandler {
	
	@Autowired
	TestLinkClient testLinkClient
	
	@Autowired(required=false)
	ICacheManagementService cacheManagementService


	@Value('${testlink.projectName:}')
	String projectName
	
	@Value('${item.filter:tlAllFilter}')
	private String itemFilter

	def currentItems
	
	int page = 0
	
	Date currentTimestamp = new Date()
	
	TestSuite[] testSuites
	
	private void allSuites() {
		TestProject project = testLinkClient.getTestProjectByName(projectName)
		TestSuite[] topSuites = testLinkClient.getFirstLevelTestSuitesForTestProject(project.id)
		List<TestSuite> suiteList = new ArrayList<TestSuite>()
		topSuites.each { TestSuite parent ->
			suiteList.add(parent)
			addSuites(parent, suiteList)
		}
		testSuites = suiteList.toArray(new TestSuite[suiteList.size()])
	}
	
	private TestSuite[] addSuites(TestSuite parent, List<TestSuite> suiteList) {
		TestSuite[] children = []
		try {
			children = testLinkClient.getTestSuitesForTestSuite(parent.id)
		} catch (e) {}
		children.each { TestSuite child  ->
			suiteList.add(child)
			addSuites(child, suiteList)
		}
	}
	
	public def getItems() {
		if (!testSuites) {
			allSuites()
		}
		if (testSuites.length > 0) {
			TestCase[] tc = testLinkClient.getTestCasesForTestSuite(testSuites[0].id, false, 'full')
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
		if (testSuites.length > 0 && page < testSuites.length-2) {
			return testSuites[page+1].name
		}
		return null
	}

	public Object nextPage() {
		page++
		if (testSuites.length > 0 && page < testSuites.length) {
			TestCase[] tc = testLinkClient.getTestCasesForTestSuite(testSuites[page].id, false, 'full')
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
