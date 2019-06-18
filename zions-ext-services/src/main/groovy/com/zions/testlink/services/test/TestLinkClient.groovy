package com.zions.testlink.services.test

import br.eti.kinoshita.testlinkjavaapi.TestLinkAPI
import br.eti.kinoshita.testlinkjavaapi.constants.ResponseDetails
import br.eti.kinoshita.testlinkjavaapi.constants.TestCaseDetails
import br.eti.kinoshita.testlinkjavaapi.constants.TestCaseStepAction
import br.eti.kinoshita.testlinkjavaapi.model.Attachment
import br.eti.kinoshita.testlinkjavaapi.model.Build
import br.eti.kinoshita.testlinkjavaapi.model.CustomField
import br.eti.kinoshita.testlinkjavaapi.model.Execution
import br.eti.kinoshita.testlinkjavaapi.model.Platform
import br.eti.kinoshita.testlinkjavaapi.model.TestCase
import br.eti.kinoshita.testlinkjavaapi.model.TestCaseStep
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan
import br.eti.kinoshita.testlinkjavaapi.model.TestProject
import br.eti.kinoshita.testlinkjavaapi.model.TestSuite
import br.eti.kinoshita.testlinkjavaapi.util.TestLinkAPIException
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
@Traceable
class TestLinkClient {

	TestLinkAPI delegate

	String testLinkUrl

	String devToken

	public TestLinkClient(@Value('${testlink.url:}') String testLinkUrl,
	@Value('${testlink.token:}') String token) {
		this.testLinkUrl = testLinkUrl
		this.devToken = token
		delegate = new TestLinkAPI(testLinkUrl, devToken)
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
	public TestCase[] getTestCasesForTestSuite(Integer testSuiteId, Boolean deep, TestCaseDetails detail)
	throws TestLinkAPIException {
		return delegate.getTestCasesForTestSuite(testSuiteId, deep, detail);
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
	 * Retrieves the platforms of a test project.
	 *
	 * @param projectId test project ID
	 * @return platforms array
	 * @throws TestLinkAPIException if an error occurs when retrieving the platforms
	 */
	public Platform[] getProjectPlatforms(Integer projectId) throws TestLinkAPIException {
		return delegate.getProjectPlatforms(projectId);
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
		return delegate.getTestCase(testCaseId, testCaseExternalId, version);
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
