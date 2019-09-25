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
 * <img src="AGenericRestClient.png"/>
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
	RESTClient delegate;
	
	boolean checked = false
	
	@Value('${output.test.data.flag:false}')
	boolean outputTestDataFlag = false
	
	@Value('${output.test.data.location:./src/test/resources/testdata}')
	String outputTestDataLocation

	@Value('${output.test.data.type:json}')
	String outputTestDataType
	
	@Value('${output.test.data.prefix:testdata}')
	String outputTestDataPrefix
	
	
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
			
			delegate.client.getCredentialsProvider().setCredentials(
				new AuthScope(proxyHost, Integer.parseInt(proxyPort)),
				new UsernamePasswordCredentials(proxyUser, proxyPassword)
			)
			delegate.setProxy(proxyHost, Integer.parseInt(proxyPort), 'http')
			
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
	
	
	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#get(java.util.Map)
	 */
	@Override
	def get(Map input) {
		//log.debug("GenericRestClient::get -- URI before checkBlankCollection: "+input.uri)
		String url
		def query = null
		if (outputTestDataFlag) {
			url = "${input.uri}"
			if (input.query) {
				query = input.query
			}
		}
		boolean withHeader = false
		if (input.withHeader) {
			withHeader = input.withHeader
		}
		input.remove('withHeader')
		Map oinput = input
		if (checked) {
			oinput = checkBlankCollection(input)
		}
		//log.debug("GenericRestClient::get -- URI after checkBlankCollection: "+oinput.uri)
		HttpResponseDecorator resp = delegate.get(oinput)
		if (resp.data == null) {
			log.warn("GenericRestClient::get -- Failed. Status: "+resp.getStatusLine());
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
	
	def writeTestData(def data, String url, def query) {
		long ts = new Date().time
		if (this.outputTestDataType == 'json') {
			File dir = new File(this.outputTestDataLocation)
			File of = new File(dir, "${this.outputTestDataPrefix}${ts}.json")
			def os = of.newDataOutputStream()
			def info = [url: "${url}", query: query, type: "${this.outputTestDataType}", data: "new JsonBuilder(data).toPrettyString()}"]
			os << "${new JsonBuilder(info).toPrettyString()}";
			os.close()
		} else if (this.outputTestDataType == 'xml') {
			File dir = new File(this.outputTestDataLocation)
			File of = new File(dir, "${this.outputTestDataPrefix}${ts}.json")
			def os = of.newDataOutputStream()
			def info = [url: "${url}", query: query, type: "${this.outputTestDataType}", data: "${new XmlUtil().serialize(data)}"]
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
		if (input.withHeader) {
			withHeader = input.withHeader
		}
		input.remove('withHeader')
		Map oinput = input
		if (checked) {
			oinput = checkBlankCollection(input)
		}
		HttpResponseDecorator resp = delegate.put(oinput)
		
		if (resp.status != 200) {
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
		Map oinput = input
		if (checked) {
			oinput = checkBlankCollection(input)
		}
		HttpResponseDecorator resp = delegate.delete(oinput)
		if (resp.status != 204) {
			return null;
		}
		return resp.data
	}
	
	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#patch(java.util.Map)
	 */
	@Override
	def patch(Map input) {
		boolean withHeader = false
		if (input.withHeader) {
			withHeader = input.withHeader
		}
		input.remove('withHeader')
		Map oinput = input
		if (checked) {
			oinput = checkBlankCollection(input)
		}
		def sinput = null
		try {
			sinput = deepcopy(oinput)
		} catch (e) {}
		HttpResponseDecorator resp = delegate.patch(oinput)
		
		if (resp.status != 200) {
			log.error("GenericRestClient::patch -- Warning. Status: "+resp.getStatusLine());
			if (sinput) {
				String json = new JsonBuilder(sinput).toPrettyString()
				log.error("Input data: ${json}");
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
		boolean withHeader = false
		if (input.withHeader) {
			withHeader = input.withHeader
		}
		input.remove('withHeader')
		Map oinput = input
		if (checked) {
			oinput = checkBlankCollection(input)
		}
		def sinput = null
		try {
			sinput = deepcopy(oinput)
		} catch (e) {}
		HttpResponseDecorator resp = delegate.post(oinput)
		//JsonOutput t
		if (resp.status != 200 && resp.status!= 201) {
			
			log.error("GenericRestClient::post -- Failed. Status: "+resp.getStatusLine());
			if (sinput) {
				String json = new JsonBuilder(sinput).toPrettyString()
				log.error("Input data: ${json}");
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
		return resp.data;
	}
	
	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#checkBlankCollection(java.util.Map)
	 */
	def checkBlankCollection(Map input) {
		String uri = "${input.uri}"
		String checkedUri = "${this.tfsUrl}//"
		if (this.tfsUrl && uri.startsWith(checkedUri) ) {
		
			uri = "${this.tfsUrl}/${uri.substring(checkedUri.length())}"
			input.uri = uri
		}
		if (input.headers != null && input.headers.Referer != null) {
			String refUri = input.headers.Referer
			if (refUri.startsWith(checkedUri)) {
				refUri = "${this.tfsUrl}/${refUri.substring(checkedUri.length())}"
				input.headers.Referer = refUri
			}
		}
		return input
	}

	public Object rateLimitPost(Map input, Closure encoderFunction = null) {
		boolean withHeader = false
		if (input.withHeader) {
			withHeader = input.withHeader
		}
		input.remove('withHeader')
		Map oinput = input
		if (checked) {
			oinput = checkBlankCollection(input)
		}
		def currentEncoder = null
		if (encoderFunction) {
			currentEncoder = delegate.encoder.'application/json'
			delegate.encoder.'application/json' = encoderFunction
			
		}
		Map retryCopy = deepcopy(oinput)
		try {
			HttpResponseDecorator resp = delegate.post(oinput)
		} catch (IllegalArgumentException e) {
			log.error("rateLimitPost failed with input ${oinput.toString()} because: ${e}")
			return
		}

		if (currentEncoder) {
			delegate.encoder.'application/json' = currentEncoder
			
		}

		Header dHeader = resp.getLastHeader('x-ratelimit-delay')
		int status = resp.status
		if ((status == 200 || status == 201) && dHeader != null) {
			log.error "GenericRestClient::rateLimitPost --  ADO started throttling. Delaying 5 minutes."
			System.sleep(300000)			
		}
		//JsonOutput t
		if (resp.status != 200 && resp.status != 201) {
			log.error("GenericRestClient::rateLimitPost -- Failed. Status: "+resp.getStatusLine());
			if (retryCopy) {
				String json = new JsonBuilder(retryCopy).toPrettyString()
				log.error("Input data: ${json}");
			}
			System.sleep(300000)
			resp = delegate.post(retryCopy)
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
		def bos = new ByteArrayOutputStream()
		def oos = new ObjectOutputStream(bos)
		oos.writeObject(orig); oos.flush()
		def bin = new ByteArrayInputStream(bos.toByteArray())
		def ois = new ObjectInputStream(bin)
		return ois.readObject()
   }
}
