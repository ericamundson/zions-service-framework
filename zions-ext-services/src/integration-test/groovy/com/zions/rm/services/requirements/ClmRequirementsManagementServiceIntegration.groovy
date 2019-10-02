package com.zions.rm.services.requirements

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.test.context.ContextConfiguration

import com.mongodb.MongoClient
import com.zions.clm.services.rest.ClmBGenericRestClient
import com.zions.clm.services.rest.ClmGenericRestClient
import com.zions.common.services.attachments.IAttachments
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.cache.MongoDBCacheManagementService
import com.zions.common.services.cacheaspect.CacheInterceptor
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.db.DatabaseQueryService
import com.zions.common.services.db.IDatabaseQueryService
import com.zions.common.services.mongo.EmbeddedMongoBuilder
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService
import com.zions.common.services.test.SpockLabeler
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Specification
import spock.mock.DetachedMockFactory
import com.mongodb.diagnostics.logging.Loggers;

import java.util.logging.Level;
import java.util.logging.Logger;

@ContextConfiguration(classes=[ClmRequirementsManagementServiceSpecConfig])
class ClmRequirementsManagementServiceIntegration extends Specification implements SpockLabeler {
	
	@Autowired
	DataGenerationService dataGenerationService
	
	@Autowired
	ClmRequirementsManagementService underTest
	
	@Autowired
	ClmRequirementsItemManagementService clmRequirementsItemManagementService
	
	@Autowired
	ICacheManagementService cacheManagementService
	
	@Autowired
	IDatabaseQueryService databaseQueryService
	
	@Autowired
	IAttachments attachmentService
	
	@Autowired
	IGenericRestClient rmGenericRestClient
	
	def 'Handle base requirement artifacts'() {
		given: 'A page of requirement artifacts'
		cacheManagementService.cacheModule = "RM"
		cacheManagementService.dbProject = 'coretest'
		attachmentService.sendAttachment(_) >> {}
		Date ts = new Date()
		def items = underTest.queryDatawarehouseSource(ts)
		def tItems = dataGenerationService.generate('/testdata/rmFirstQueryResult.json')
		items.addAll(tItems)
				
		when: w_ 'produce artifact data for text or non-text types'
		int tCount = 0
		int ntCount = 0
		items.each { rmItem ->
			String sid = "${rmItem.reference_id}"
			//sometimes this is blank?  some kind of error!
			if (sid) {
				int id = Integer.parseInt(sid)
				//log.debug("items.each loop for id: ${sid}")
				String primaryTextString = "${rmItem.text}"
				//data warehouse indicator for wrapperresources is replacing the primay text field with this string
				String format = primaryTextString.equals("No primary text") ? 'WrapperResource' : 'Text'
				//here is the uri
				String about = "${rmItem.about}"
				ClmArtifact artifact = new ClmArtifact('', format, about)
				if (format == 'Text') {
					underTest.getTextArtifact(artifact,false,true)
					tCount++
				}
				else if (format == 'WrapperResource'){
					underTest.getNonTextArtifact(artifact,true)
					ntCount++
				}
				
				def reqChanges = clmRequirementsItemManagementService.getChanges('', artifact, [:])
			}
		}

		
		then: t_ 'processing has no failures and verify set of text vs non-text artifact data elements count'
		tCount > 0 || ntCount > 0
		
	}
	
	@Ignore
	def 'Handle module requirement artifacts'() {
		given: 'A set of module artifacts'
		//resourceURI=_Z2_j8fQqEeihN7TNly_siw
		String rmQuery = 'resourceURI=_Z2_j8fQqEeihN7TNly_siw'
		def moduleUris = underTest.queryForModules(rmQuery)
		int size = moduleUris.size()
		moduleUris.removeRange(1, size)
		
		when: w_ 'Processed for module artifact details'
		//rmGenericRestClient.outputTestDataFlag = true
		rmGenericRestClient.outputTestDataType = 'xml'
		rmGenericRestClient.outputTestDataPrefix = 'module'
		def modules = []
		moduleUris.each { moduleUri ->
			ClmRequirementsModule module = underTest.getModule(moduleUri,false)
			int errCount = validateModule(module) 
			if (errCount == 0){
				modules.add(module)
			}
		}
		rmGenericRestClient.outputTestDataFlag = false
		
		then: t_ 'Validate module artifacts'
		modules.size() > 0
	}
	

	def 'Get all links for artifact'() {
		setup: 'get artifact'
		def artifact = dataGenerationService.generate('/testdata/requirementWithLinks.xml')
		
		when: w_ 'determine all requirement artifact links'
		def links = underTest.getAllLinks('7543', new Date(), artifact.artifact)
		
		then: t_ 'validate number of links are 7'
		links.size() == 7
	}
	
	def 'Partial flush of query pages'() {
		given: 'A valid data warehouse query'
		
		when: w_ 'Run a partial flush of two pages of requirements artifact query'
		boolean success = true
		try {
			underTest.flushQueries(false,2)
		} catch (e) {
			e.printStackTrace()
			success = false
		}
		def pages = cacheManagementService.getAllOfType('DataWarehouseQueryData')
		
		then: t_ 'Validate no errors and there are two pages of requirements artifacts'
		success && pages.size() == 2
		
		cleanup:
		cacheManagementService.clear()

	}
	def validateModule(def module)	{
		def errCount = 0
		if (module.orderedArtifacts == null || module.orderedArtifacts.size() < 1) {
			log.error("*** ERROR: No elements found in module ${module.getTitle()}")
			errCount++
		}
		else {
			module.checkForDuplicates()
		}
		// Check all artifacts for "Heading"/"Row type" inconsistencies, then abort on this module if any were found
		module.orderedArtifacts.each { artifact ->
			if ((artifact.getIsHeading() && artifact.getArtifactType() != 'Heading') ||
				(!artifact.getIsHeading() && artifact.getArtifactType() == 'Heading') ) {
				log.error("*** ERROR: Artifact #${artifact.getID()} has inconsistent heading row type in module ${module.getTitle()}")
				errCount++
			}
			else if (artifact.getIsHeading() && (artifact.hasEmbeddedImage() || artifact.getFormat() == 'WrapperResource')) {
				log.error("*** ERROR: Artifact #${artifact.getID()} is heading with image or attachment in module ${module.getTitle()}")
				errCount++
			}
			else if (artifact.getIsDuplicate()) {
				log.error("*** ERROR: Artifact #${artifact.getID()} is a duplicate instance in module ${module.getTitle()}.  This is not yet supported in ADO.")
				errCount++
			}
		}
		return errCount
	}

}

@TestConfiguration
@Profile("integration-test")
@ComponentScan(["com.zions.common.services.cacheaspect", "com.zions.ext.services", "com.zions.common.services.restart", "com.zions.common.services.cache.db", "com.zions.common.services.test","com.zions.rm.services.requirements.handlers"])
@PropertySource("classpath:integration-test.properties")
@EnableMongoRepositories(basePackages = "com.zions.common.services.cache.db")
public class ClmRequirementsManagementServiceSpecConfig {
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
	
	@Value('${tfs.user:none}')
	String tfsUserid
	
	@Value('${clm.password:none}')
	String password
	
	@Value('${cache.location}')
	String cacheLocation
	
	@Value('${sql.resource.name}')
	String sqlResourceName
	
	@Bean
	IAttachments attachmentService() {
		IAttachments o = factory.Stub(IAttachments);
		return o
	}

	@Bean
	ClmRequirementsManagementService underTest() {
		return new ClmRequirementsManagementService()
	}
	
	@Bean
	ClmRequirementsItemManagementService clmRequirementsItemManagementService() {
		return new ClmRequirementsItemManagementService()
	}
	
	@Bean
	RequirementsMappingManagementService rmMappingManagementService() {
		return new RequirementsMappingManagementService()
	}

	@Bean
	ClmRequirementsFileManagementService rmRequirementsFileManagementService()
	{
		return new ClmRequirementsFileManagementService()
	}
	
	@Bean
	ICacheManagementService cacheManagementService() {
		//return new CacheManagementService(cacheLocation)
		return new MongoDBCacheManagementService()
	}
	
	@Bean
	IGenericRestClient rmGenericRestClient() {
		return new ClmGenericRestClient(clmUrl, userid, password)
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
	DataGenerationService dataGenerationService() {
		return new DataGenerationService()
	}
	
	@Bean
	IDatabaseQueryService databaseQueryService() {
		return factory.Spy(DatabaseQueryService)
	}


	@Value('${spring.data.mongodb.host:utmsdev0598}')
	String dbHost

	@Value('${spring.data.mongodb.database:adomigration_dev}')
	String database

	@Bean
	MongoClient mongoClient() throws UnknownHostException {
		Logger.getLogger(Loggers.PREFIX).setLevel(Level.OFF);
		def builder = new EmbeddedMongoBuilder()
			.version('3.2.16')
			//.tempDir('mongodb')
			.installPath('../zions-common-data/mongodb/win32/mongodb-win32-x86_64-3.2.16/bin')
			.bindIp("localhost")
			.port(12345)
			.build();
		return builder
	}
	
	public @Bean MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoClient(), database);
	}
}
