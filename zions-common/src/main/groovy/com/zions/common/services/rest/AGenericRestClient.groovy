package com.zions.common.services.rest

import org.apache.http.Header
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.springframework.beans.factory.annotation.Value
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient

/**
 * Main implementation for all ReST interaction within framework.
 * 
 * <p><b>Design:</b></p>
 * <img src="AGenericRestClient.svg"/>
 * 
 * @author z091182
 *
 * @startuml
 * abstract class AGenericRestClient [[java:com.zions.common.services.rest.AGenericRestClient]] {
 * 	~RESTClient delegate
 * 	~boolean checked
 * 	+{abstract}void setCredentials(String user, String token)
 * 	~def setProxy()
 * 	~def get(Map input)
 * 	~def put(Map input)
 * 	~def delete(Map input)
 * 	~def patch(Map input)
 *  ~def post(Map input)
 * 	~def checkBlankCollection(Map input)
 * 	+Object rateLimitPost(Map input)
 * 	~def deepcopy()
 * }
 * interface IGenericRestClient [[java:com.zions.common.services.rest.IGenericRestClient]] {
 * }
 * IGenericRestClient <|.. AGenericRestClient
 * AGenericRestClient --> groovyx.net.http.RESTClient: delegate
 * @enduml
 */
@Slf4j
abstract class AGenericRestClient implements IGenericRestClient {
	ARESTClient delegate;
		
	@Value('${output.test.data.flag:false}')
	boolean outputTestDataFlag = false
	
	@Value('${output.test.data.location:./src/test/resources/testdata}')
	String outputTestDataLocation

	@Value('${output.test.data.type:json}')
	String outputTestDataType
	
	@Value('${output.test.data.prefix:testdata}')
	String outputTestDataPrefix
	
	@Value('${web.scheme:http}')
	String webScheme
	
	@Value('${retry.copy:false}')
	boolean rcopy
	
	
	@Override
	abstract public void setCredentials(String user, String token);

	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#setProxy()
	 */
	@Override
	def setProxy() {
		String proxyHost = System.getProperty("proxy.Host")
		if (proxyHost == null) {
			loadFromEnv()
			proxyHost = System.getProperty("proxy.Host")
		}
		if (proxyHost != null) {
			String proxyPort = System.getProperty("proxy.Port")
			String proxyUser = System.getProperty("proxy.User")
			String proxyPassword = System.getProperty("proxy.Password")
			if (proxyUser != null) {
				delegate.client.getCredentialsProvider().setCredentials(
					new AuthScope(proxyHost, Integer.parseInt(proxyPort)),
					new UsernamePasswordCredentials(proxyUser, proxyPassword)
				)
			}
			delegate.setProxy(proxyHost, Integer.parseInt(proxyPort), webScheme)
			
		}
	}
	
	def loadFromEnv() {
		String host = System.getenv('proxy.Host')
		if (host) { System.setProperty('proxy.Host', host) }
		String port = System.getenv('proxy.Port')
		if (port) { System.setProperty('proxy.Port', port) }
		String user = System.getenv('proxy.User')
		if (user) { System.setProperty('proxy.User', user) }
		String password = System.getenv('proxy.Password')
		if (password) { System.setProperty('proxy.Password', password) }
	}
	
	
	Map fixUri(Map input) {
		String uri = "${input.uri}"
		if (uri !== null) {
			uri = uri.replace('+', '%20').replace(' ', '%20')
			input.uri = uri
		}
		return input
	}
	
	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#get(java.util.Map)
	 */
	@Override
	def get(Map input, Closure parserFunction = null) {
		//log.debug("GenericRestClient::get -- URI before checkBlankCollection: "+input.uri)
		String url
		def query = null
		input = fixUri( input )
		if (outputTestDataFlag) {
			url = "${input.uri}"
			if (input.query) {
				query = input.query
			}
			if ("${input.contentType}".contains('json')) {
				this.outputTestDataType = 'json'
			} else if ("${input.contentType}".contains('xml')) {
				this.outputTestDataType = 'xml'
				
			}
		}
		def currentParser = null
		if (parserFunction) {
			String contentType = 'application/json'
			if (input.contentType) {
				contentType  = "${input.contentType}"
			}
			currentParser = delegate.parser."${contentType}"
			delegate.parser."${contentType}" = parserFunction
			
		}
		boolean withHeader = false
		if (input.withHeader) {
			withHeader = input.withHeader
		}
		input.remove('withHeader')
		//log.debug("GenericRestClient::get -- URI after checkBlankCollection: "+oinput.uri)
		HttpResponseDecorator resp = delegate.get(input)
		if (parserFunction || currentParser) {
			String contentType = 'application/json'
			if (input.contentType) {
				contentType  = "${input.contentType}"
			}
			delegate.parser."${contentType}" = currentParser
			
		}
		try {
			// if get ReST call to ADO tesults in a 'xxxNotFoundException', resp.data will be null and 
			// the following will cause a NullPointerException to be thrown causing execution to fail
			if (resp.data == null) {
				//log.warn("GenericRestClient::get -- Failed. Status: "+resp.getStatusLine());
			}
		} catch (NullPointerException npe) {
			return null
		}
		int status = resp.status
		Header dHeader = resp.getLastHeader('x-ratelimit-delay')
		if ((status == 200 || status == 201) && dHeader != null) {
			log.error "GenericRestClient::get --  ADO started throttling. Delaying 10 second."
			System.sleep(10000)
			throw new ThrottleException("Throttled: http ${status}")
		}

		if (withHeader) {
			def headerMap = [:]
			resp.allHeaders.each { Header header ->
				headerMap[header.name] = header.value 
			}
			def result = [data: resp.data, headers: headerMap]
			return result
		}
		if (outputTestDataFlag) {
			writeTestData(resp.data, url, query)
		}
		return resp.data;
	}
	
	def writeTestData(def data, String url, def query, def method = 'get', def body = null) {
		long ts = new Date().time
		if (this.outputTestDataType == 'json') {
			File dir = new File(this.outputTestDataLocation)
			File of = new File(dir, "${this.outputTestDataPrefix}${ts}.json")
			def os = of.newDataOutputStream()
			def info = [method: method, url: "${url}", query: query, type: "${this.outputTestDataType}"]
			if (data) {
				info.data = "${new JsonBuilder(data).toPrettyString()}"
			}
			if (body) {
				info.body = body
			}
			os << "${new JsonBuilder(info).toPrettyString()}";
			os.close()
		} else if (this.outputTestDataType == 'xml') {
			File dir = new File(this.outputTestDataLocation)
			File of = new File(dir, "${this.outputTestDataPrefix}${ts}.json")
			def os = of.newDataOutputStream()
			
			def info = [method: method, url: "${url}", query: query, type: "${this.outputTestDataType}"]
			if (data) {
				info.data = "${new XmlUtil().serialize(data)}"
			}
			if (body) {
				info.body = body
			}
			os << "${new JsonBuilder(info).toPrettyString()}";
			os.close()
		}
	}
	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#get(java.util.Map)
	 */
	

	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#put(java.util.Map)
	 */
	@Override
	def put(Map input) {
		boolean withHeader = false
		input = fixUri( input )
		if (input.withHeader) {
			withHeader = input.withHeader
		}
		input.remove('withHeader')
		HttpResponseDecorator resp = delegate.put(input)
		
		if (resp.status != 200 && resp.status != 204) {
			log.error("GenericRestClient::put -- Failed. Status: "+resp.getStatusLine());
			return null;
		}
		if (withHeader) {
			def headerMap = [:]
			resp.allHeaders.each { Header header ->
				headerMap[header.name] = header.value 
			}
			def result = [data: resp.data, headers: headerMap]
			return result
		}
		return resp.data;
	}
	
	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#delete(java.util.Map)
	 */
	@Override
	def delete(Map input) {
		input = fixUri( input )
		HttpResponseDecorator resp = delegate.delete(input)
		if (resp.status != 204) {
			return null;
		}
		return resp.data
	}
	
	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#patch(java.util.Map)
	 */
	@Override
	def patch(Map input, Closure handleResponse = null) {
		input = fixUri( input )
		boolean withHeader = false
		if (input.withHeader) {
			withHeader = input.withHeader
		}
		input.remove('withHeader')
		def sinput = null
		try {
			sinput = deepcopy(input)
		} catch (e) {}
		HttpResponseDecorator resp = delegate.patch(input)

		int status = resp.status
		if ((status == 412 || status == 409) && handleResponse) {
		return handleResponse(resp)
		}
		
		
		Header dHeader = resp.getLastHeader('x-ratelimit-delay')
		if ((status == 200 || status == 201) && dHeader != null) {
			log.error "GenericRestClient::patch --  ADO started throttling. Delaying 1 minutes."
			System.sleep(10000)			
			throw new ThrottleException("Throttled: http ${status}")
		}
		if (status != 200) {
			log.error("GenericRestClient::patch -- Warning. Status: "+resp.getStatusLine());
			if (status == 408) {
				System.sleep(20000)
				throw new ThrottleException("Throttled: http ${status}")
				
			}
			if (sinput) {
				String json = new JsonBuilder(sinput).toPrettyString()
				log.error("Input data: ${json}");
				if (status == 503) {
					log.error("Starting retry!")
					System.sleep(30000)
					resp = delegate.post(sinput)
					if (resp.status != 200) {
						log.error("Failed retry!")
					} else {
						log.error("Finished retry!")
					}
				}
			}
			return null;
		}
		if (withHeader) {
			def headerMap = [:]
			resp.allHeaders.each { Header header ->
				headerMap[header.name] = header.value 
			}
			def result = [data: resp.data, headers: headerMap]
			return result
		}
		return resp.data;
	}

	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#post(java.util.Map)
	 */
	@Override
	def post(Map input) {
		input = fixUri( input )
		String url
		def query = null
		def body = null
		if (outputTestDataFlag) {
			url = "${input.uri}"
			if (input.query) {
				query = input.query
			}
			if (input.body) {
				body = input.body
			}
		}
		boolean withHeader = false
		if (input.withHeader) {
			withHeader = input.withHeader
		}
		input.remove('withHeader')
		def sinput = null
		try {
			sinput = deepcopy(input)
		} catch (e) {}
		HttpResponseDecorator resp = delegate.post(input)
		//JsonOutput t
		
		int status = resp.status
		if (status != 200 && status != 201 && status != 204) {
			
			log.error("GenericRestClient::post -- Failed. Status: "+resp.getStatusLine());
			if (sinput) {
				String json = new JsonBuilder(sinput).toPrettyString()
				log.error("Input data: ${json}");
				if (resp.status == 503) {
					log.error("Starting retry!")
					System.sleep(30000)
					resp = delegate.post(sinput)
					if (resp.status != 200 && resp.status!= 201) {
						log.error("Failed retry!")
					} else {
						log.error("Finished retry!")
					}
				}
			}
		}
		if (withHeader) {
			def headerMap = [:]
			resp.allHeaders.each { Header header ->
				headerMap[header.name] = header.value 
			}
			def result = [data: resp.data, headers: headerMap]
			return result
		}
		if (outputTestDataFlag) {
			writeTestData(resp.data, url, query, 'post', body)
		}
		return resp.data;
	}
	

	public Object rateLimitPost(Map input,  Closure encoderFunction = null) {
		input = fixUri( input )
		boolean withHeader = false
		if (input.withHeader) {
			withHeader = input.withHeader
		}
		input.remove('withHeader')
		def currentEncoder = null
		if (encoderFunction) {
			String requestContentType = 'application/json'
			if (input.requestContentType) {
				requestContentType  = "${input.requestContentType}"
			}
			currentEncoder = delegate.encoder."${requestContentType}"
			delegate.encoder."${requestContentType}" = encoderFunction
			
		}
		Map retryCopy
		if (rcopy) {
			try {
				retryCopy = deepcopy(input)
			} catch (e) {}
		}
		HttpResponseDecorator resp
		try {
			resp = delegate.post(input)
		} catch (e) {
			log.error "GenericRestClient::rateLimitPost --  Response error: ${e.message}"
			System.sleep(10000)			
			throw new ThrottleException("Response issue: http ${e.message}")
		} finally {
			if (encoderFunction || currentEncoder) {
				String requestContentType = 'application/json'
				if (input.requestContentType) {
					requestContentType  = "${input.requestContentType}"
				}
				delegate.encoder."${requestContentType}" = currentEncoder
				
			}
		}


		Header dHeader = resp.getLastHeader('x-ratelimit-delay')
		int status = resp.status
		if ((status == 200 || status == 201) && dHeader != null) {
			log.error "GenericRestClient::rateLimitPost --  ADO started throttling. Delaying 1 minutes."
			System.sleep(10000)			
			if (encoderFunction || currentEncoder) {
				String requestContentType = 'application/json'
				if (input.requestContentType) {
					requestContentType  = "${input.requestContentType}"
				}
				delegate.encoder."${requestContentType}" = currentEncoder
				
			}
			throw new ThrottleException("Throttled: http ${status}")
		}
		//JsonOutput t
		if (status != 200 && status != 201) {
			log.error("GenericRestClient::rateLimitPost -- Failed. Status: "+resp.getStatusLine());
			if (retryCopy) {
				String json = new JsonBuilder(retryCopy).toPrettyString()
				log.error("Input data: ${json}");
			} 
			if (status == 413) {
				if (encoderFunction || currentEncoder) {
					String requestContentType = 'application/json'
					if (input.requestContentType) {
						requestContentType  = "${input.requestContentType}"
					}
					delegate.encoder."${requestContentType}" = currentEncoder
					
				}
				return null
			}
			//if (status == 408 || status == 503) {
				System.sleep(20000)
				if (encoderFunction || currentEncoder) {
					String requestContentType = 'application/json'
					if (input.requestContentType) {
						requestContentType  = "${input.requestContentType}"
					}
					delegate.encoder."${requestContentType}" = currentEncoder
					
				}
				throw new ThrottleException("Throttled: http ${resp.status}")
				
			//}
//			try {
//				resp = delegate.post(retryCopy)
//			} catch (e) {
//				throw e
//			} finally {
//				if (encoderFunction || currentEncoder) {
//					String requestContentType = 'application/json'
//					if (oinput.requestContentType) {
//						requestContentType  = "${oinput.requestContentType}"
//					}
//					delegate.encoder."${requestContentType}" = currentEncoder
//					
//				}
//			}
		}
		if (withHeader) {
			def headerMap = [:]
			resp.allHeaders.each { Header header ->
				headerMap[header.name] = header.value 
			}
			def result = [data: resp.data, headers: headerMap]
			return result
		}
		return resp.data;
	}
	
	def deepcopy(orig) {
//		def bos = new ByteArrayOutputStream()
//		def oos = new ObjectOutputStream(bos)
//		oos.writeObject(orig); oos.flush()
//		def bin = new ByteArrayInputStream(bos.toByteArray())
//		def ois = new ObjectInputStream(bin)
		return orig.clone()
   }
}
