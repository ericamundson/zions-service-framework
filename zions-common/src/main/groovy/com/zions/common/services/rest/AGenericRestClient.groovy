package com.zions.common.services.rest

import org.apache.http.Header
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient

abstract class AGenericRestClient implements IGenericRestClient {
	private RESTClient delegate;

	@Override
	abstract public void setCredentials(String user, String token);

	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#setProxy()
	 */
	@Override
	def setProxy() {
		String proxyHost = System.getProperty("proxy.Host")
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
	
	
	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#get(java.util.Map)
	 */
	@Override
	def get(Map input) {
		//log.debug("GenericRestClient::get -- URI before checkBlankCollection: "+input.uri)
		Map oinput = checkBlankCollection(input)
		//log.debug("GenericRestClient::get -- URI after checkBlankCollection: "+oinput.uri)
		HttpResponseDecorator resp = delegate.get(oinput)
		return resp.data;
	}
	
	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#put(java.util.Map)
	 */
	@Override
	def put(Map input) {
		Map oinput = checkBlankCollection(input)
		HttpResponseDecorator resp = delegate.put(oinput)
		
		if (resp.status != 200) {
			return null;
		}
		return resp.data;
	}
	
	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#delete(java.util.Map)
	 */
	@Override
	def delete(Map input) {
		Map oinput = checkBlankCollection(input)
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
		Map oinput = checkBlankCollection(input)
		HttpResponseDecorator resp = delegate.patch(oinput)
		
		if (resp.status != 200) {
			return null;
		}
		return resp.data;
	}

	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#post(java.util.Map)
	 */
	@Override
	def post(Map input) {
		Map oinput = checkBlankCollection(input)
		HttpResponseDecorator resp = delegate.post(oinput)
		//JsonOutput t
		if (resp.status != 200) {
			log.debug("GenericRestClient::post -- Failed. Status: "+resp.getStatusLine());
		}
		return resp.data;
	}
	
	/* (non-Javadoc)
	 * @see com.zions.vsts.services.tfs.rest.IGenericRestClient#checkBlankCollection(java.util.Map)
	 */
	def checkBlankCollection(Map input) {
		String uri = "${input.uri}"
		String checkedUri = "${this.tfsUrl}//"
		if (uri.startsWith(checkedUri) ) {
		
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

	public Object rateLimitPost(Map input) {
		Map oinput = checkBlankCollection(input)
		Map retryCopy = deepcopy(oinput)
		HttpResponseDecorator resp = delegate.post(oinput)
		
		Header dHeader = resp.getLastHeader('x-ratelimit-delay')
		if (resp.status == 200 && dHeader != null) {
			System.sleep(300000)			
		}
		//JsonOutput t
		if (resp.status != 200 && resp.status != 201) {
			log.error("GenericRestClient::post -- Failed. Status: "+resp.getStatusLine());
			System.sleep(300000)
			resp = delegate.post(retryCopy)
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
