package com.zions.common.services.restart

import static org.junit.Assert.*

import com.zions.common.services.test.SpockLabeler
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.stereotype.Component
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ContextConfiguration(classes=[RestartManagementServiceConfig])
class RestartManagementServiceSpec extends Specification {
	
	@Autowired
	RestartManagementService underTest
	
	@Autowired
	ICheckpointManagementService checkpointManagementService
	
	@Autowired
	TestQueryHandler testQueryHandler

	def 'processPhases happy path'() {
		given: 'stub check point selection'
		Checkpoint cp = new Checkpoint([checkpointId: 0, pageUrl: 'http://theUrl', phase: 'test', logEntries:[]])
		1 * checkpointManagementService.selectCheckpoint(_) >> cp
		
//		and: 'stub checkpoint management service addCheckpoint'
//		1 * checkpointManagementService.addCheckpoint(_, _)

		and: 'stub query handler getItems'
		1 * testQueryHandler.getItems() >> [url: 'https://stuff']
		
		and: 'stub query handler initialUrl'
		1 * testQueryHandler.initialUrl() >> 'http://notit'
		
		and: 'stub query handler getPageUrl to match checkpoint returns url'
		1 * testQueryHandler.getPageUrl() >> 'http://theUrl'
		
		and: 'stub query handler nextPage'
		1 * testQueryHandler.nextPage() >> [[entry:[stuff:'stuff']]]
		
		and: 'stub checkpoint management service addCheckpoint'
		1 * checkpointManagementService.addCheckpoint(_, _)
		
		and: 'stub query handler getPageUrl'
		1 * testQueryHandler.getPageUrl() >> null
		
		
		and: 'stub query handler nextItems to return null'
		1 * testQueryHandler.nextPage() >> null
		
		when: 'call method under test (processPhases)'
		boolean flag = true
		try {
			underTest.processPhases { phase, items -> 
				println "${phase} with ${items}"
			}
		} catch (e) {
			flag = false
		}
		then: "Success == ${flag}"
		flag
	}

}


@TestConfiguration
@Profile("test")
//@ComponentScan(["com.zions.common.services.restart"])
@PropertySource("classpath:test.properties")
class RestartManagementServiceConfig {
	def factory = new DetachedMockFactory()
	
	@Bean
	CheckpointManagementService checkpointManagementService()
	{
		return factory.Mock(CheckpointManagementService)
	}
	
	@Bean
	RestartManagementService underTest() {
		return new RestartManagementService()
	}
	
	@Bean
	TestQueryHandler testQueryHandler() {
		return factory.Mock(TestQueryHandler)
	}
	
//	@Bean
//	Map<String, IQueryHandler> queryHandlers() {
//		return ['testQueryHandler': factory.Mock(TestQueryHandler)]
//	}
	
}

@Component
class TestQueryHandler implements IQueryHandler {

	@Override
	public Object getItems() {
		
		return null;
	}

	@Override
	public String initialUrl() {
		
		return null;
	}

	@Override
	public String getPageUrl() {
		
		return null;
	}

	@Override
	public Object nextPage() {
		
		return null;
	}

	@Override
	public String getFilterName() {
		
		return null;
	}


	@Override
	public boolean isModified(Object item) {
		
		return true;
	}
	
}