package com.zions.vsts.services.tfs.rest

import static org.junit.Assert.*

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.rest.ARESTClient
import com.zions.common.services.rest.ThrottleException
import com.zions.common.services.test.SpockLabeler
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import org.apache.http.Header
import org.apache.http.StatusLine
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import spock.lang.Ignore
import spock.lang.Specification
import org.springframework.spring.*


class GenericRestClientSpecTest extends Specification {
	
	IGenericRestClient genericRestClient
	ARESTClient delegate
	

	public void setup() {
		delegate = Mock(ARESTClient)
		genericRestClient = new GenericRestClient(delegate)
	}	

	public void 'call get with successful result'() {
		given: 'make delegate calls'
		HttpResponseDecorator resp = Mock(HttpResponseDecorator)
		1 * delegate.get(_) >> resp
		2* resp.getData() >> [stuff: 'stuff']
		
		when: 'call method under test'
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "http://vsts/none/_apis/wit/workitems",
			headers: [Accept: 'application/json'],
			query: ['api-version': '4.1', '\$expand': 'all' ]
			) 
			
		then: 'result.stuff == stuff'
		"${result.stuff}" == 'stuff'
		
			
	}

	public void 'call get with failed result'() {
		given: 'make delegate calls'
		HttpResponseDecorator resp = Mock(HttpResponseDecorator)
		1 * delegate.get(_) >> resp
		2* resp.getData() >> null
		
		when: 'call method under test'
		def result = genericRestClient.get(
			contentType: ContentType.JSON,
			uri: "http://vsts/none/_apis/wit/workitems",
			headers: [Accept: 'application/json'],
			query: ['api-version': '4.1', '\$expand': 'all' ]
			) 
			
		then: 'result == null'
		result == null
		
			
	}
	
	public void 'call put with successful result'() {
		given: 'make delegate calls'
		HttpResponseDecorator resp = Mock(HttpResponseDecorator)
		1 * delegate.put(_) >> resp
		1* resp.getStatus() >> 200
		1* resp.getData() >> [stuff: 'stuff']
		
		when: 'call method under test'
		def result = genericRestClient.put(
			contentType: ContentType.JSON,
			uri: "http://vsts/none/_apis/wit/workitems",
			headers: [Accept: 'application/json'],
			query: ['api-version': '4.1', '\$expand': 'all' ]
			) 
			
		then: 'result.stuff == stuff'
		"${result.stuff}" == 'stuff'
	}
	
	public void 'call put with bad status'() {
		given: 'stub internal delegate calls'
		HttpResponseDecorator resp = Mock(HttpResponseDecorator)
		1 * delegate.put(_) >> resp
		1* resp.getStatus() >> 400
		
		when: 'call method under test'
		def result = genericRestClient.put(
			contentType: ContentType.JSON,
			uri: "http://vsts/none/_apis/wit/workitems",
			headers: [Accept: 'application/json'],
			query: ['api-version': '4.1', '\$expand': 'all' ]
			) 
			
		then: 'result == null'
		result == null
	}
	
	public void 'call delete with good status'() {
		given: 'stub internal delegate calls'
		HttpResponseDecorator resp = Mock(HttpResponseDecorator)
		1 * delegate.delete(_) >> resp
		1 * resp.getStatus() >> 204
		1 * resp.getData() >> "stuff"
		
		when: 'call method under test'
		def result = genericRestClient.delete(
			contentType: ContentType.JSON,
			uri: "http://vsts/none/_apis/wit/workitems",
			headers: [Accept: 'application/json'],
			query: ['api-version': '4.1', '\$expand': 'all' ]
			) 
			
		then: "result == 'stuff'"
		result == 'stuff'
	}
	
	public void 'call delete with bad status'() {
		given: 'stub internal delegate calls'
		HttpResponseDecorator resp = Mock(HttpResponseDecorator)
		1 * delegate.delete(_) >> resp
		1 * resp.getStatus() >> 400
		
		when: 'call method under test'
		def result = genericRestClient.delete(
			contentType: ContentType.JSON,
			uri: "http://vsts/none/_apis/wit/workitems",
			headers: [Accept: 'application/json'],
			query: ['api-version': '4.1', '\$expand': 'all' ]
			) 
			
		then: 'result == null'
		result == null
	}
	
	public void 'call patch with successful result'() {
		given: 'stub internal delegate calls'
		HttpResponseDecorator resp = Mock(HttpResponseDecorator)
		1 * delegate.patch(_) >> resp
		1* resp.getStatus() >> 200
		1* resp.getData() >> [stuff: 'stuff']
		
		when: 'make call under test'
		def result = genericRestClient.patch(
			contentType: ContentType.JSON,
			uri: "http://vsts/none/_apis/wit/workitems",
			headers: [Accept: 'application/json'],
			query: ['api-version': '4.1', '\$expand': 'all' ]
			) 
			
		then: "result.stuff == 'stuff'"
		"${result.stuff}" == 'stuff'
	}
	
	public void 'call patch with bad status'() {
		given: 'stub internal delegate calls'
		HttpResponseDecorator resp = Mock(HttpResponseDecorator)
		1 * delegate.patch(_) >> resp
		1 * resp.getStatus() >> 400
		
		when: 'call method under test'
		def result = genericRestClient.patch(
			contentType: ContentType.JSON,
			uri: "http://vsts/none/_apis/wit/workitems",
			headers: [Accept: 'application/json'],
			query: ['api-version': '4.1', '\$expand': 'all' ]
			) 
			
		then: 'result == null'
		result == null
	}
	
	public void 'call post with successful result'() {
		given: 'stub internal delegate calls'
		HttpResponseDecorator resp = Mock(HttpResponseDecorator)
		1 * delegate.post(_) >> resp
		1* resp.getStatus() >> 200
		1* resp.getData() >> [stuff: 'stuff']
		
		when: 'make call under test'
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "http://vsts/none/_apis/wit/workitems",
			headers: [Accept: 'application/json'],
			query: ['api-version': '4.1', '\$expand': 'all' ]
			) 
			
		then: "result.stuff == 'stuff'"
		"${result.stuff}" == 'stuff'
	}
	@Ignore
	public void 'call post with bad status'() {
		given: 'stub internal delegate calls'
		HttpResponseDecorator resp = Mock(HttpResponseDecorator)
		1 * delegate.post(_) >> resp
		1* resp.getStatus() >> 413
		1* resp.getStatus() >> 200
		
		when: 'call method under test'
		def result = genericRestClient.post(
			contentType: ContentType.JSON,
			uri: "http://vsts/none/_apis/wit/workitems",
			headers: [Accept: 'application/json'],
			query: ['api-version': '4.1', '\$expand': 'all' ]
			) 
			
		then: 'result == null'
		result == null
	}
	
	public void 'call rateLimitPost data success'() {
		given: 'stub internal delegate calls'
		HttpResponseDecorator resp = Mock(HttpResponseDecorator)
		1 * delegate.post(_) >> resp
		1 * resp.getLastHeader(_) >> null
		1 * resp.getStatus() >> 200
		1 * resp.getData() >> [stuff: 'stuff']
		
		when: 'call method under test'
		def result = genericRestClient.rateLimitPost(
			contentType: ContentType.JSON,
			uri: "http://vsts/none/_apis/wit/workitems",
			headers: [Accept: 'application/json'],
			query: ['api-version': '4.1', '\$expand': 'all' ]
			) 
			
		then: "result.stuff == 'stuff'"
		"${result.stuff}" == 'stuff'
	}
	
	public void 'call rateLimitPost started throttle check'() {
		given: 'stub internal delegate calls'
		System.metaClass.static.sleep = { long ms -> 
			return true
		}
		HttpResponseDecorator resp = Mock(HttpResponseDecorator)
		Header header = Mock(Header)
		1 * delegate.post(_) >> resp
		1 * resp.getLastHeader(_) >> header
		1 * resp.getStatus() >> 200
		//1 * resp.getData() >> [stuff: 'stuff']
		
		when: 'call method under test'
		boolean flag = false
		try {
			def result = genericRestClient.rateLimitPost(
				contentType: ContentType.JSON,
				uri: "http://vsts/none/_apis/wit/workitems",
				headers: [Accept: 'application/json'],
				query: ['api-version': '4.1', '\$expand': 'all' ]
				) 
		} catch (ThrottleException t) {
			flag = true
		}
		
		then: "result.stuff == 'stuff'"
		flag
	}
	
	public void 'call rateLimitPost bad status'() {
		given: 'stub internal delegate calls'
		System.metaClass.static.sleep = { long ms -> 
			return true
		}
		HttpResponseDecorator resp = Mock(HttpResponseDecorator)
		Header header = Mock(Header)
		StatusLine line = Mock(StatusLine)
		1 * delegate.post(_) >> resp
		1 * resp.getLastHeader(_) >> header
		1 * resp.getStatus() >> 413
		1 * resp.getStatusLine() >> line
		1 * line.toString() >> "Batch Failed"
		//1 * resp.getData() >> null
		
		when: 'call method under test'
		def result = genericRestClient.rateLimitPost(
			contentType: ContentType.JSON,
			uri: "http://vsts/none/_apis/wit/workitems",
			headers: [Accept: 'application/json'],
			query: ['api-version': '4.1', '\$expand': 'all' ]
			) 
			
		then: "result == null"
		result == null
	}
}

