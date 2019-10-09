package com.zions.clm.services.rest

import static org.junit.Assert.*

import com.zions.common.services.test.SpockLabeler
import groovy.xml.XmlUtil
import groovyx.net.http.ContentType
import org.apache.http.client.utils.URIBuilder
import org.junit.Test

import spock.lang.Specification

class ClmGenericRestClientSpec extends Specification {


	def 'test for rm folders'()  {
//		given: 'setup URIBuilder'
//		URIBuilder builder = new URIBuilder('https://clm.cs.zionsbank.com/rm/folders')
//		//builder.addParameter('oslc.where', 'public_rm:parent=https://clm.cs.zionsbank.com/rm/folders/_mxVp8L1REeS5FIAyBUGhBQ')
//		String parmVal = 'public_rm:parent=https://clm.cs.zionsbank.com/rm/folders/_mxVp8L1REeS5FIAyBUGhBQ'
//		String body = "oslc.where=${parmVal}"
//		
//		ClmGenericRestClient client = new ClmGenericRestClient('https://clm.cs.zionsbank.com/rm', 'z091182', '4878Middy001')
//		
//		when: 'class get on client'
//		def currentParser = client.delegate.parser.'application/xml'
//		client.delegate.parser.'application/rdf+xml' = currentParser
//		def response = client.get(
//			//requestContentType: ContentType.TEXT,
//			withHeader: 'true',
//			contentType: 'application/xml',
//			uri: 'https://clm.cs.zionsbank.com/rm/folders?oslc.where=public_rm:parent=https://clm.cs.zionsbank.com/rm/folders/_mxVp8L1REeS5FIAyBUGhBQ',
//			//body: body,
//			headers: ['Accept': 'application/rdf+xml', 'OSLC-Core-Version': '2.0']
//		)
//		String xml = XmlUtil.serialize(response.data)
//		println xml
//		
//		then:
//		true
	}

}
