package com.zions.testlink.services.cli.action.test

import static org.junit.Assert.*

import br.eti.kinoshita.testlinkjavaapi.model.Attachment
import br.eti.kinoshita.testlinkjavaapi.model.Execution
import br.eti.kinoshita.testlinkjavaapi.model.ExecutionStatus
import br.eti.kinoshita.testlinkjavaapi.model.ExecutionType
import br.eti.kinoshita.testlinkjavaapi.model.TestCase
import br.eti.kinoshita.testlinkjavaapi.model.TestCaseStep
import br.eti.kinoshita.testlinkjavaapi.model.TestPlan
import br.eti.kinoshita.testlinkjavaapi.model.TestProject
import br.eti.kinoshita.testlinkjavaapi.model.TestSuite
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.data.ldap.repository.config.EnableLdapRepositories
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.test.context.ContextConfiguration

import com.mongodb.MongoClient
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.mongo.EmbeddedMongoBuilder
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.restart.IRestartManagementService
import com.zions.common.services.restart.RestartManagementService
import com.zions.common.services.test.Generator
import com.zions.common.services.test.SpockLabeler
import com.zions.mr.services.rest.MrGenericRestClient
import com.zions.testlink.services.test.TestLinkAttachmentManagementService
import com.zions.testlink.services.test.TestLinkClient
import com.zions.testlink.services.test.TestLinkItemManagementService
import com.zions.testlink.services.test.TestLinkMappingManagementService
import com.zions.vsts.services.test.TestManagementService
import com.zions.vsts.services.tfs.rest.MultiUserGenericRestClient
import com.zions.vsts.services.work.FileManagementService
import com.zions.vsts.services.work.WorkManagementService
import spock.lang.Ignore
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[TranslateTestLinkToADOSpecConfig])
class TranslateTestLinkToADOSpec extends Specification {
	
	@Autowired
	Map<String, Generator> generators
	
	@Autowired
	TranslateTestLinkToADO underTest
	
	@Autowired
	TestLinkClient testLinkClient
	
	@Autowired
	TestLinkAttachmentManagementService testLinkAttachmentManagementService
	
	@Autowired
	TestManagementService testManagementService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	IRestartManagementService restartManagementService
	
	int id = 600000
		
	@Ignore
	public void 'Simulate all phases plus clean'() {
		setup: 'TestLink stubs'
		cacheManagementService.cacheModule = 'TL'
		restartManagementService.includePhases = 'testcase,plans,links,executions,attachments'
		//testManagementService.cleanupTestItems('', 'IntegrationTests', "IntegrationTests\\testlink")
		setupTestLinkStubs()
		
		when: 'call execute'
		def appArgs = new DefaultApplicationArguments(loadTLArgs())
		underTest.execute(appArgs)
		
		then: 'result.size() = 100'
		def result = cacheManagementService.getAllOfType(ICacheManagementService.RESULT_DATA)
		result.size() == 100
		
		cleanup: 'Cleanup ADO data'		
		testManagementService.cleanupTestItems('', 'IntegrationTests', "IntegrationTests\\testlink")
		
	}
	
	private setupTestLinkStubs() {
		id = 600000
		TestProject project = new TestProject()
		project.name = generators['quoteGenerator'].gen()
		project.id = id
		project.notes = generators['quoteGenerator'].gen()
		id++
		
		List<TestPlan> plans = []
		while (id < 600003) {
			TestPlan plan = new TestPlan()
			plan.id = id
			plan.name = generators['quoteGenerator'].gen()
			plans.add(plan)
			id++
		}
		List<TestSuite> suites = []
		while (id < 600013) {
			TestSuite suite = new TestSuite()
			suite.id = id
			suite.name = generators['quoteGenerator'].gen()
			suites.add(suite)
			id++
		}
		
		List<TestCase> testcase = []
		while (id < 600130) {
			
			TestCase tc = new TestCase()
			tc.id = id
			tc.name = generators['quoteGenerator'].gen()
			for (int i = 0; i<2; i++) {
				TestCaseStep ts = new TestCaseStep()
				ts.id = Integer.parseInt("${tc.id}${i}")
				ts.actions = generators['quoteGenerator'].gen()
				ts.expectedResults = generators['quoteGenerator'].gen()
				ts.executionType = ExecutionType.MANUAL
				tc.steps.add(ts)
			}
			testcase.add(tc)
			id++
		}
		testLinkClient.getTestProjectByName(_) >> {
			return project
		}
		
		testLinkClient.getProjectTestPlans(_) >> {
			return plans.toArray(new TestPlan[2])
		}
		
		testLinkClient.getFirstLevelTestSuitesForTestProject(_) >> {
			List<TestSuite> os = suites.findAll { TestSuite s ->
				s.id < 600005
			}
			return os.toArray(new TestSuite[os.size()])
		}
		
		testLinkClient.getTestSuitesForTestSuite(_) >> { args ->
			Integer suiteId = args[0]
			if (suiteId == 600003) {
				List<TestSuite> csuites = suites.findAll { TestSuite csuite ->
					csuite.id > 600004 && csuite.id < 600009
					 
				}
				return csuites.toArray(new TestSuite[csuites.size()])
			} else if (suiteId == 600004) {
				List<TestSuite> csuites = suites.findAll { TestSuite csuite ->
					csuite.id >= 600009
					 
				}
				return csuites.toArray(new TestSuite[csuites.size()])
				
			}
			return []
		}
		testLinkClient.getTestCasesForTestSuite(_,_,_) >> { args ->
			Integer suiteId = args[0]
			int s = ((suiteId - 600002) * 10) + 600013
			List<TestCase> otestcase = testcase.findAll { TestCase tc -> 
				tc.id > (s - 10) && tc.id <= s
			}
			return otestcase.toArray(new TestCase[otestcase.size()])
		}

		testLinkClient.getFirstLevelSuitesForTestPlan(_) >> { args ->
			TestPlan plan = args[0]
			if (plan.id == 600001) {
				List<TestSuite> top = suites.findAll { TestSuite ts ->
					ts.id == 600003
				}
				return top.toArray(new TestSuite[1])
			}
			List<TestSuite> top = suites.findAll { TestSuite ts ->
				ts.id == 600004
			}
			return top.toArray(new TestSuite[1])
		}
		
		testLinkClient.getSuitesForTestPlanSuites(_, _) >> { args ->
			Integer planId = args[0]
			Integer suiteId = args[1]
			if (suiteId == 600003) {
				List<TestSuite> csuites = suites.findAll { TestSuite csuite ->
					csuite.id > 600004 && csuite.id < 600009
					 
				}
				return csuites.toArray(new TestSuite[csuites.size()])
			} else if (suiteId == 600004) {
				List<TestSuite> csuites = suites.findAll { TestSuite csuite ->
					csuite.id >= 600009
					 
				}
				return csuites.toArray(new TestSuite[csuites.size()])
				
			}
			return []
		}
		
		testLinkClient.getTestCasesForTestPlanSuite(_, _, _, _) >> { args ->
			Integer suiteId = args[0]
			int s = ((suiteId - 600002) * 10) + 600013
			List<TestCase> otestcase = testcase.findAll { TestCase tc -> 
				tc.id > (s - 10) && tc.id <= s
			}
			return otestcase.toArray(new TestCase[otestcase.size()])
		}
		
		testLinkClient.getTestCase(_, _, _) >> { args ->
			int id = args[0]
			TestCase tcs = testcase.find { TestCase tc ->
				tc.id == id
			}
			return tcs
		}
		testLinkClient.getTestCasesForTestPlan(_, _, _, _, _, _, _, _, _, _) >> { args ->
			int planId = args[0]
			if (planId == 600001) {
				List<TestCase> tco = testcase.findAll { TestCase t ->
					(t.id > 600013 && t.id <= 600023) || (t.id > 600033 && t.id <= 600073)
				}
				return tco.toArray(new TestCase[tco.size])
			}
			List<TestCase> tco = testcase.findAll { TestCase t ->
				(t.id > 600023 && t.id <= 600033) || (t.id > 600073 && t.id <= 600113)
			}
			return tco.toArray(new TestCase[tco.size])
		}
		String[] results = ['n','p','f','b']
		int length = results.size()
		Random r = new Random()
		
		//Execution results
		testLinkClient.getLastExecutionResult(_, _, _) >> { args ->
			int planId = args[0]
			int testcaseId = args[1]
			int i = r.nextInt(length)
			char ar = results[i]
			Execution e = new Execution()
			e.id = generators['integerGenerator'].gen()
			e.notes = generators['quoteGenerator'].gen()
			e.executionType = ExecutionType.MANUAL
			e.status = ExecutionStatus.getExecutionStatus(ar)
			e.testPlanId = planId
			return e
		}
		
		testLinkClient.getTestCaseAttachments(_, _) >> { args ->
			int id = args[0]
			if ((id % 5) == 0) {
				File att = new File('./src/test/resources/testdata/Negative News - Keywords List.docx')
				if (att) {
					String data = Base64.encoder.encodeToString(att.bytes)
					Attachment attachment = new Attachment()
					attachment.content = data
					attachment.fileName = 'Negative New - Keywords List.docx'
					attachment.id = generators['integerGenerator'].gen()
					return [attachment]
				}
			}
			return []
		}
	}

	private String[] loadTLArgs() {
		String[] args = [
			'--testlink.projectName=Integration',
			'--tfs.areapath=IntegrationTests\\testlink',
			'--tfs.project=IntegrationTests',
			'--tfs.url=https://dev.azure.com/eto-dev',
			'--test.mapping.file=./src/test/resources/testdata/NORKOMTLMapping.xml',
			'--tl.query=none',
			'--wi.filter=allfilter',
			'--tl.filter=tlAllFilter',
			'--include.update=phases',
			'--include.phases=testcase,plans,links,executions',
			'--meta=meta',
			'--refresh=true'
		]
		return args
	}
}




@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.vsts.services", "com.zions.common.services.test", "com.zions.testlink.services.test.handlers", "com.zions.common.services.restart", "com.zions.testlink.services.cli.action.test", "com.zions.common.services.cacheaspect","com.zions.common.services.ldap", "com.zions.common.services.user", "com.zions.common.services.cache.db", "com.zions.common.services.cache"])
@PropertySource("classpath:test.properties")
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
@EnableLdapRepositories(basePackages = "com.zions.common.services.ldap")
class TranslateTestLinkToADOSpecConfig {
	def factory = new DetachedMockFactory()
	@Bean
	IGenericRestClient genericRestClient() {
		return new MultiUserGenericRestClient()
	}

	@Bean
	JavaMailSender sender() {
		return new JavaMailSenderImpl()
	}
	
	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}

	
	@Bean
	WorkManagementService workManagementService() {
		return new WorkManagementService()
	}

	@Bean
	FileManagementService fileManagementService() {
		return new FileManagementService()
	}

	@Bean
	IGenericRestClient mrGenericRestClient() {
		return new MrGenericRestClient('none', 'none')
	}

	// TestLink services
	@Bean 
	TranslateTestLinkToADO underTest() {
		return new TranslateTestLinkToADO()
	}
	
	@Bean
	TestLinkClient testLinkClient() {
		return factory.Stub(TestLinkClient)
	}
	
	@Bean
	TestLinkAttachmentManagementService testLinkAttachmentManagementService() {
		return new TestLinkAttachmentManagementService()
	}
	
	@Bean 
	TestLinkItemManagementService testLinkItemManagementService() {
		return new TestLinkItemManagementService() 
	}
	
	@Bean
	TestLinkMappingManagementService testLinkMappingManagementService() {
		return new TestLinkMappingManagementService()
	}

	//Cache managment beans
	public MongoClient mongoClient() throws Exception {
		
		return new EmbeddedMongoBuilder()
			.version('3.2.16')
			//.tempDir('mongodb')
			.installPath('../zions-common-data/mongodb/win32/mongodb-win32-x86_64-3.2.16/bin')
			.bindIp("localhost")
			.port(12345)
			.build();
	}

	
	public @Bean MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoClient(), getDatabaseName());
	}

	protected String getDatabaseName() {
		return 'tldev';
	}
	
	@Bean
	IRestartManagementService restartManagementService() {
		return new RestartManagementService()
	}

	
	@Bean
	ICacheManagementService cacheManagementService() {
		return new MongoDBCacheManagementService()
	}
	
	//LDAP
	@Value('${ldap.url:}')
	String ldapUrl
	@Value('${ldap.partitionSuffix:}')
	String ldapPartitionSuffix
	@Value('${ldap.principal:}')
	String ldapPrincipal
	@Value('${ldap.password:}')
	String ldapPassword

	@Bean
	public LdapContextSource contextSource() {
		LdapContextSource contextSource = new LdapContextSource();
		 
		contextSource.setUrl(ldapUrl);
		contextSource.setBase(ldapPartitionSuffix);
		contextSource.setUserDn(ldapPrincipal);
		contextSource.setPassword(ldapPassword);
		 
		return contextSource;
	}
	
	@Bean
	public LdapTemplate ldapTemplate() {
		return new LdapTemplate(contextSource());
	}


}
