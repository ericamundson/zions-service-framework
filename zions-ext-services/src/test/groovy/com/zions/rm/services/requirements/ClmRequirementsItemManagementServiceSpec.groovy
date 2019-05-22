package com.zions.rm.services.requirements

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.test.context.ContextConfiguration

import com.mongodb.MongoClient
import com.zions.clm.services.rest.ClmBGenericRestClient
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.mongo.EmbeddedMongoBuilder
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ClmRequirementsItemManagementServiceSpecConfig])
class ClmRequirementsItemManagementServiceSpec extends Specification {
	
	@Autowired
	ClmRequirementsItemManagementService underTest
	
	@Autowired
	IGenericRestClient rmGenericRestClient
	
	@Autowired
	ClmRequirementsManagementService clmRequirementsManagementService
	
	@Autowired
	DataGenerationService dataGenerationService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	ClmRequirementsFileManagementService clmRequirementsFileManagementService
	
	@Autowired
	IAttachments attachmentService


	/**
	 * This test scenario is using a whole bunch of test data for stubbing clm rest calls.
	 * 
	 * Mostly stubbing with real data because design is not too testable.
	 * 
	 * Thus, not too BDD.  Although not much of our unit testing is.
	 */
	def 'Main flow for module with related artifacts while testing ADO change data processing'() {
		setup: "Setup a module and module's related artifacts"
		setupModuleAndRelatedArtifactData()
		cacheManagementService.cacheModule = 'RM'
		
		when: 'Run behavior to get changes for module and related artifacts'
		String murl = "https://clm.cs.zionsbank.com/rm/resources/_frOPQFDzEeWblrEplHqOHQ"
		def module = clmRequirementsManagementService.getModule(murl, false)
		def memberMap = [:]
		def mchanges = underTest.getChanges('IntegrationTests', module, memberMap)
		def aid = module.getCacheID()
				
		def ubound = module.orderedArtifacts.size() - 1
		def lastSection = 0
		def artifactChanges = []
		0.upto(ubound, {

			// If Heading is immediately followed by Supporting Material, move Heading title to Supporting Material and logically delete Heading artifact
			if (module.orderedArtifacts[it].getIsHeading() &&
				it < module.orderedArtifacts.size()-1 &&
				isToIncorporateTitle(module,it+1)) {
				
				module.orderedArtifacts[it+1].setTitle(module.orderedArtifacts[it].getTitle())
				module.orderedArtifacts[it].setIsDeleted(true)
				return  // Skip Heading artifact
			}
			else if (module.orderedArtifacts[it].getIsHeading()) {
				module.orderedArtifacts[it].setDescription('') // If simple heading, remove duplicate description
			}
			// Only store first occurrence of an artifact in the module
			if (!module.orderedArtifacts[it].getIsDuplicate()) {
				def changes = underTest.getChanges('IntegrationTests', module.orderedArtifacts[it], memberMap)
				artifactChanges.add(changes)
				
			}
		})
		
		then: 'Validate module and related artifact changes'
		mchanges != null && artifactChanges.size() == 191
		
	}
	
	boolean isToIncorporateTitle(def module, def indexOfElementToCheck) {
		// This function is dependent upon the type of module
		String artifactType = module.orderedArtifacts[indexOfElementToCheck].getArtifactType()
		String moduleType = module.getArtifactType()
		boolean shouldMerge = false
		if (artifactType == 'Supporting Material') { // regardless of module
			shouldMerge = true
		}
		else if ((moduleType == 'Functional Spec') &&
			   (artifactType == 'Scope' ||
				artifactType == 'Out of Scope' ||
				artifactType == 'Assumption' ||
				artifactType == 'Issue' )) {
			shouldMerge = true
		}
		else if ((moduleType == 'UI Spec') &&
			   (artifactType == 'Screen Change' ||
				artifactType == 'User Interface Flow'))	{
			shouldMerge = true
		}
		return shouldMerge
	}

	void setupModuleAndRelatedArtifactData() {
		URI uri = this.getClass().getResource('/testdata').toURI()
		File tDir = new File(uri)
		def moduleFiles = []
		tDir.eachFile { File file ->
			if (file.name.startsWith('module')) moduleFiles.add(file)
		}
		def moduleMap = [:]
		moduleFiles.each { File module ->
			def modData = dataGenerationService.generate(module)
			String url = "${modData.url}"
			def data = new XmlSlurper().parseText(modData.data)
			moduleMap[url] = data
		}
		
		rmGenericRestClient.get(_) >> { args ->
			def input = args[0]
			String auri = "${input.uri}"
			return moduleMap[auri]
		}
		
		attachmentService.sendAttachment(_) >> {}
		
		clmRequirementsFileManagementService.ensureRequirementFileAttachment(_, _) >> {}
	}

}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.rm.services.requirements.handlers", "com.zions.common.services.cacheaspect", "com.zions.ext.services", "com.zions.common.services.restart", "com.zions.common.services.cache.db", "com.zions.common.services.test"])
@PropertySource("classpath:test.properties")
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
class ClmRequirementsItemManagementServiceSpecConfig {
	def factory = new DetachedMockFactory()
	
	@Value('${clm.url:https://clm.cs.zionsbank.com}')
	String clmUrl
	
	@Value('${clm.user:none}')
	String clmUser
	
	@Value('${clm.password:none}')
	String clmPassword
	
	@Value('${mr.url:none}')
	String mrUrl
	
	@Value('${clm.user:none}')
	String userid
	
	@Bean
	IAttachments attachmentService() {
		return factory.Stub(IAttachments)
	}
	
	@Bean
	ClmRequirementsFileManagementService clmRequirementsFileManagementService() {
		return factory.Stub(ClmRequirementsFileManagementService)
	}

	@Bean
	ICacheManagementService cacheManagementService() {
		//return new CacheManagementService(cacheLocation)
		return new MongoDBCacheManagementService()
	}


	@Bean
	ClmRequirementsManagementService clmRequirementsManagementService() {
		return new ClmRequirementsManagementService()
	}
	
	@Bean
	RequirementsMappingManagementService rmMappingManagementService() {
		return new RequirementsMappingManagementService()
	}

	
	
	@Bean
	IGenericRestClient rmGenericRestClient() {
		return factory.Stub(ClmGenericRestClient)
	}
	
	
	@Bean
	IGenericRestClient rmBGenericRestClient() {
		return new ClmBGenericRestClient(clmUrl, clmUser, clmPassword)
	}
	
	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}
	@Bean
	ClmRequirementsItemManagementService underTest() {
		return new ClmRequirementsItemManagementService()
	}
	
	
	@Bean
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}


	
	@Value('${spring.data.mongodb.database:adomigration_dev}')
	String database
	
	@Bean
	MongoClient mongoClient() throws UnknownHostException {
		def builder = new EmbeddedMongoBuilder()
			.version('3.2.16')
			//.tempDir('mongodb')
			.installPath('../zions-common-data/mongodb/win32/mongodb-win32-x86_64-3.2.16/bin')
			.bindIp("localhost")
			.port(12345)
			.build();
		//Logger.getLogger(Loggers.PREFIX).setLevel(Level.SEVERE);
		return builder
	}
	
	public @Bean MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoClient(), database);
	}

}
