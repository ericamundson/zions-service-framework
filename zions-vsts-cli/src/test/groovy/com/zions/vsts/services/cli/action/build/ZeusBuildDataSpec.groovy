package com.zions.vsts.services.cli.action.build

import static org.junit.Assert.*

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.test.context.ContextConfiguration
import com.zions.common.services.cache.CacheManagementService
import com.zions.common.services.cache.ICacheManagementService
import com.zions.common.services.command.CommandManagementService
import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.test.DataGenerationService
import com.zions.vsts.services.build.BuildManagementService
import com.zions.vsts.services.tfs.rest.GenericRestClient
import com.zions.vsts.services.work.WorkManagementService
import groovy.json.JsonSlurper
import spock.lang.Ignore
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[ZeusBuildDataSpecConfig])
class ZeusBuildDataSpec extends Specification {
	@Autowired
	ZeusBuildData zeusBuildData
	
	@Autowired
	IGenericRestClient genericRestClient
	
	@Autowired
	DataGenerationService dataGenerationService

	//@Ignore
	def 'execute standard flow'() {
		setup: 'test data for stubs'
		URI uri = this.getClass().getResource('/testdata').toURI()
		File tDir = new File(uri)
		def testFiles = []
		tDir.eachFile { File file ->
			if (file.name.startsWith('zeusbuilddata')) testFiles.add(file)
		}
		
		and: 'set execute arguments'
		URI zuri = this.getClass().getResource('/Zeus').toURI()
		File ztDir = new File(zuri)
		String zPath = ztDir.absolutePath
		def appArgs = new DefaultApplicationArguments(loadArgs(zPath))

		and: 'ADO rest data'		
		def rMap = [:]
		testFiles.each { File f ->
			def request = dataGenerationService.generate(f)
			def data = new JsonSlurper().parseText(request.data)
			String url = "${request.url}"
			rMap[url] = data
		}
				
		and: 'stub genericRestClient calls'
		genericRestClient.get(_) >> { args ->
			def d = args[0]
			String auri = "${d.uri}"
			if (!auri.startsWith('https://dev.azure')) {
				auri = "https://dev.azure.com/zionseto${d.uri}"
			}
			def out = rMap[auri]
			return out
		}
		
		when: 'run execute'
		boolean success = true
		try {
			zeusBuildData.execute(appArgs)
		} catch (e) {
			e.printStackTrace()
			success = false
		}
		
		then: 'No exception'
		success
	}
	
	private String[] loadArgs(String iRepoDir) {
		String[] args = [
			'--tfs.url=https://dev.azure.com/zionseto', 
			'--tfs.user=svc-cloud-vsmigration',
			'--tfs.token=me6cvg6ggjbhbcy4aj5v2xb7hnfvjtfzf4bud6wf5wkmxe2z6slq',
			'--tfs.project=Zeus', 
			'--build.id=15677',
			'--out.dir=build/', 
			'--change.request={{change.request}}',
			"--in.repo.dir=${iRepoDir}",
			'--out.repo.dir=build/repo', 
			'--release.date={{release.date}}'
		]
		return args
	}

}

@TestConfiguration
@Profile("test")
@ComponentScan(["com.zions.vsts.services", "com.zions.common.services.test"])
@PropertySource("classpath:test.properties")
class ZeusBuildDataSpecConfig {
	def factory = new DetachedMockFactory()
	
	@Autowired
	@Value('${cache.location:cache}')
	String cacheLocation

	@Bean
	ZeusBuildData zeusBuildData() {
		return new ZeusBuildData()
	}
	@Bean
	JavaMailSender sender() {
		return new JavaMailSenderImpl()
	}
	
	@Bean
	BuildManagementService buildManagementService() {
		return new BuildManagementService()
	}
	
	@Bean
	WorkManagementService workManagementService() {
		return new WorkManagementService()
	}
	
	@Bean 
	ICacheManagementService cacheManagementService() {
		return new CacheManagementService(cacheLocation)
	}
	
	@Bean
	CommandManagementService commandManagementService() {
		return new CommandManagementService();
	}
	
	@Bean
	IGenericRestClient genericRestClient() {
		return factory.Stub(GenericRestClient)
	}
	
	@Bean
	IGenericRestClient mrGenericRestClient() {
		return factory.Stub(GenericRestClient)
	}
	
}
