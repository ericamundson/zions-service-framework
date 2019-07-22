package com.zions.testlink.services.test

import br.eti.kinoshita.testlinkjavaapi.TestLinkAPI
import br.eti.kinoshita.testlinkjavaapi.TestLinkAPIException
import br.eti.kinoshita.testlinkjavaapi.model.Attachment
import br.eti.kinoshita.testlinkjavaapi.model.Build
import br.eti.kinoshita.testlinkjavaapi.model.CustomField
import br.eti.kinoshita.testlinkjavaapi.model.Execution
import br.eti.kinoshita.testlinkjavaapi.model.ExecutionType
import br.eti.kinoshita.testlinkjavaapi.model.Platform
import br.eti.kinoshita.testlinkjavaapi.model.ResponseDetails
import br.eti.kinoshita.testlinkjavaapi.model.TestCase
import br.eti.kinoshita.testlinkjavaapi.model.TestCaseStep
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan
import br.eti.kinoshita.testlinkjavaapi.model.TestProject
import br.eti.kinoshita.testlinkjavaapi.model.TestSuite
import com.zions.common.services.logging.Traceable
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Wrap TestLinkAPI calls for use with other spring boot components
 * 
 * @author z091182
 *
 */
@Component
class TestLinkClient {
	
	@Value('${testlink.projectName}')
	String projectName

	TestLinkAPI delegate

	String testLinkUrl

	String devToken
	
	def planTestCaseCache = [:]
	
	def planTestSuiteCache = [:]

	public TestLinkClient(@Value('${testlink.url:}') String testLinkUrl,
	@Value('${testlink.token:}') String token) {
		this.testLinkUrl = testLinkUrl
		URL url = new URL(testLinkUrl)
		this.devToken = token
		delegate = new TestLinkAPI(url, devToken)
	}
	

	
	/**
	 * Retrieves a Test Plan by its name.
	 *
	 * @param planName Test Plan name.
	 * @param projectName Test Project name.
	 * @return Test Plan.
	 * @throws TestLinkAPIException
	 * @since 1.0
	 */
	public TestPlan getTestPlanByName(String planName, String projectName)
	throws TestLinkAPIException
	{
		return delegate.getTestPlanByName(planName, projectName);
	}

	
	/**
	 * Retrieves an array of Test Plans associated to a Test Project.
	 *
	 * @param projectId Test Project Id.
	 * @return Array of Test Plans.
	 * @throws TestLinkAPIException
	 */
	public TestPlan[] getProjectTestPlans(Integer projectId) throws TestLinkAPIException {
		return delegate.getProjectTestPlans(projectId);
	}
	

	public TestSuite[] getFirstLevelSuitesForTestPlan(TestPlan plan) {
		TestSuite[] asuites = this.getTestSuitesForTestPlan(plan.id)
		if (asuites.size() == 0) return []
		TestProject project = this.getTestProjectByName(projectName)
		TestSuite[] psuites = this.getFirstLevelTestSuitesForTestProject(project.id)
		def psMap = [:]
		psuites.each { TestSuite s ->
			psMap[s.id] = s
		}
		
		TestSuite[] oSuites = asuites.findAll { TestSuite s -> 
			psMap.containsKey(s.id)
		}
		return oSuites
	}
	
	public TestSuite[] getSuitesForTestPlanSuites(Integer planId, Integer suiteId) {
		TestSuite[] asuites = null
		if (!planTestSuiteCache.containsKey(planId)) {
			asuites = this.getTestSuitesForTestPlan(planId)
			planTestSuiteCache[planId] = asuites
		} else {
			asuites = planTestSuiteCache[planId]
		}
		if (asuites.size() == 0) return []
		TestSuite[] oSuites = asuites.findAll { TestSuite s ->
			s.parentId == suiteId
		}
		return oSuites
	}

	/**
	 * Retrieves a Test Project by its name.
	 * 
	 * @param projectName Test Project name.
	 * @return Test Project with given name or null if not found.
	 * @throws TestLinkAPIException
	 */
	public TestProject getTestProjectByName(String projectName) throws TestLinkAPIException {
		return delegate.getTestProjectByName(projectName);
	}

	/**
	 * Get set of test suites AT TOP LEVEL of tree on a Test Project
	 *
	 * @param testProjectId
	 * @throws TestLinkAPIException
	 */
	public TestSuite[] getFirstLevelTestSuitesForTestProject(Integer testProjectId) throws TestLinkAPIException {
		return delegate.getFirstLevelTestSuitesForTestProject(testProjectId);
	}

	/**
	 * Retrieves Test Cases for a Test Suite.
	 *
	 * @param testSuiteId
	 * @param deep
	 * @param detail
	 * @return Array of Test Cases of the Test Suite.
	 * @throws TestLinkAPIException
	 */
	public TestCase[] getTestCasesForTestSuite(Integer testSuiteId, Boolean deep, String detail)
	throws TestLinkAPIException {
		TestCase[] retVal = []
		try {
			retVal = delegate.getTestCasesForTestSuite(testSuiteId, deep, detail);
			if (!(retVal instanceof TestCase[])) {
				return []
			}
		} catch (e) {}
		return retVal
	}
	
	/**
	 * Retrieves Test Cases for Test Plans.
	 * 
	 * @param testPlanId
	 * @param testCasesIds
	 * @param buildId
	 * @param keywordsIds
	 * @param keywords
	 * @param executed
	 * @param assignedTo
	 * @param executeStatus
	 * @param executionType
	 * @param getStepInfo
	 * @return Array of Test Cases of the Test Plan.
	 * @throws TestLinkAPIException
	 */
	public TestCase[] getTestCasesForTestPlan(
		Integer testPlanId, 
		List<Integer> testCasesIds, 
		Integer buildId, 
		List<Integer> keywordsIds, 
		String keywords, // , separated e.g.: database,performance
		Boolean executed, 
		List<Integer> assignedTo, 
		String executeStatus, // , separated e.g.: p,n,f
		ExecutionType executionType, 
		Boolean getStepInfo
		) 
	throws TestLinkAPIException
	{
		return delegate.getTestCasesForTestPlan(
			testPlanId, 
			testCasesIds, 
			buildId, 
			keywordsIds, 
			keywords, 
			executed, 
			assignedTo, 
			executeStatus, 
			executionType, 
			getStepInfo
		);
	}

	/**
	 * Retrieves Test Cases for a Test Suite.
	 *
	 * @param testSuiteId
	 * @param deep
	 * @param detail
	 * @return Array of Test Cases of the Test Suite.
	 * @throws TestLinkAPIException
	 */
	public TestCase[] getTestCasesForTestPlanSuite(Integer testSuiteId, Integer planId, Boolean deep, String detail)
	throws TestLinkAPIException {
		TestCase[] retVal = []
		TestCase[] ptestcase = null
		def tcMap = [:]
		if (!planTestCaseCache.containsKey(planId)) {
			ptestcase = delegate.getTestCasesForTestPlan(planId, null, null, null, null, null, null, null, null, false)
			ptestcase.each { TestCase tc ->
				tcMap[tc.id] = tc 
			}
			planTestCaseCache[planId] = tcMap
		} else {
			tcMap = planTestCaseCache[planId]
		}
		TestCase[] stestcase = getTestCasesForTestSuite(testSuiteId, deep, detail)
		retVal = stestcase.findAll { TestCase tc ->
			tcMap.containsKey(tc.id)
		}
		return retVal
	}

	/**
	 *
	 * @param testPlanId
	 * @return Array of Test Suites of Test Plan.
	 * @throws TestLinkAPIException
	 */
	public TestSuite[] getTestSuitesForTestPlan(Integer testPlanId) throws TestLinkAPIException {
		return delegate.getTestSuitesForTestPlan(testPlanId);
	}

	/**
	 * Get list of TestSuites which are DIRECT children of a given TestSuite
	 *
	 * @param testSuiteId
	 * @throws TestLinkAPIException
	 */
	public TestSuite[] getTestSuitesForTestSuite(Integer testSuiteId) throws TestLinkAPIException {
		return delegate.getTestSuitesForTestSuite(testSuiteId);
	}

	/**
	 * Retrieves all Test Projects from TestLink.
	 *
	 * @return an array of Test Projects.
	 * @throws TestLinkAPIException
	 */
	public TestProject[] getProjects() throws TestLinkAPIException {
		return delegate.getProjects();
	}

	/**
	 * Return an array of attachments of a Test Case.
	 *
	 * @param testCaseId
	 * @param testCaseExternalId
	 * @return Array of Attachments.
	 * @throws TestLinkAPIException
	 */
	public Attachment[] getTestCaseAttachments(Integer testCaseId, Integer testCaseExternalId)
	throws TestLinkAPIException {
		return delegate.getTestCaseAttachments(testCaseId, testCaseExternalId);
	}

	/**
	 * Retrieves last execution result of a Test Case.
	 *
	 * @param testPlanId
	 * @param testCaseId
	 * @param testCaseExternalId
	 * @return Last Execution.
	 * @throws TestLinkAPIException
	 */
	public Execution getLastExecutionResult(Integer testPlanId, Integer testCaseId, Integer testCaseExternalId) throws TestLinkAPIException {
		return delegate.getLastExecutionResult(testPlanId, testCaseId, testCaseExternalId);
	}

	/**
	 * Retrieves the latest Build for a given Test Plan.
	 *
	 * @param testPlanId Test Plan ID.
	 * @return Build.
	 * @throws TestLinkAPIException
	 */
	public Build getLatestBuildForTestPlan(Integer testPlanId) throws TestLinkAPIException {
		return delegate.getLatestBuildForTestPlan(testPlanId);
	}

	/**
	 * Retrieves the platforms of a test plan.
	 *
	 * @param planId test plan ID
	 * @return platforms array
	 * @throws TestLinkAPIException if an error occurs when retrieving the platforms
	 */
	public Platform[] getTestPlanPlatforms(Integer planId) throws TestLinkAPIException {
		return delegate.getTestPlanPlatforms(planId);
	}

	/**
	 * Get a test case
	 *
	 * @param testCaseId
	 * @param testCaseExternalId
	 * @param version
	 * @return Test Case.
	 * @throws TestLinkAPIException
	 */
	public TestCase getTestCase(Integer testCaseId, Integer testCaseExternalId, Integer version)
	throws TestLinkAPIException {
		TestCase tc = null
		//try {
			tc = delegate.getTestCase(testCaseId, testCaseExternalId, version);
		//} catch (e) {}
		return tc
	}
	
	/**
	 * Retrieves list of Custom Fields for a Test Case.
	 *
	 * @param testCaseId
	 * @param testCaseExternalId
	 * @param versionNumber
	 * @param testProjectId
	 * @param customFieldName
	 * @return Custom Field.
	 * @throws TestLinkAPIException
	 */
	public CustomField getTestCaseCustomFieldDesignValue(Integer testCaseId, Integer testCaseExternalId,
			Integer versionNumber, Integer testProjectId, String customFieldName, ResponseDetails details)
			throws TestLinkAPIException {
		return delegate.getTestCaseCustomFieldDesignValue(testCaseId, testCaseExternalId, versionNumber,
				testProjectId, customFieldName, details);
	}
	



}
