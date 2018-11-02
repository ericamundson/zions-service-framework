package com.zions.vsts.services.tfs.rest

import static org.junit.Assert.*

import com.zions.common.services.rest.IGenericRestClient
import groovyx.net.http.RESTClient
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import org.springframework.spring.*


@ContextConfiguration(classes = AzureContext)
class GenericRestClientSpecTest extends Specification {
	
	
	@Autowired
	IGenericRestClient genericRestClient
	

	public void 'http get with successful result'() {
		//fail("Not yet implemented")
	}

}

class AzureContext extends Specification {
	@Bean
	IGenericRestClient genericRestClient() {
		RESTClient client = Mock(RESTClient)
		return new GenericRestClient(client)
	}
}
