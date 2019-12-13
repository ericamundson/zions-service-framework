package com.zions.vsts.services.tfs.rest

import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.rest.ThrottleException
import groovy.util.logging.Slf4j
import org.apache.http.NoHttpResponseException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component('multiUserGenericRestClient')
@Slf4j
class MultiUserGenericRestClient implements IGenericRestClient {
	
	//@Autowired(required = false)
	List<GenericRestClient> genericRestClients = new ArrayList<GenericRestClient>()
	
	int currentClient = 0

	@Value('${tfs.url:}')
	String tfsUrl
	
	@Value('${tfs.users:}')
	String[] tfsUsers
	
	@Value('${tfs.tokens:}')
	String[] tfsTokens
	
	public MultiUserGenericRestClient() {
	}
	

	@Override
	public Object setProxy() {
		
		return null
	}

	@Override
	public void setCredentials(String user, String token) {
		

	}
	
	private IGenericRestClient getClient() {
		if (genericRestClients.size() == 0) {
			int i = 0
			tfsUsers.each { user ->
				GenericRestClient client = new GenericRestClient(tfsUrl, user, tfsTokens[i])
				genericRestClients.add(client)
				i++
			}
	
		}
		IGenericRestClient client = genericRestClients.get(currentClient)
		if (!client) {
			log.error "MultiUserGenericRestClient getClient::  No clients configured"
			throw new Exception("MultiUserGenericRestClient getClient:: No ADO clients configured")
		}
		if (currentClient == (genericRestClients.size() - 1)) {
			currentClient = 0
		} else {
			currentClient++
		}
		return client
	}

	@Override
	public Object get(Map input) {
		def dInput = deepcopy(input)
		def retVal
		try {
			retVal = getClient().get(input)
		} catch (ThrottleException e) {
			log.info("Current user throttled, moving to (${tfsUsers[currentClient]})")
			retVal = get(dInput);
		}
		return retVal
	}

	@Override
	public Object put(Map input) {
		
		def dInput = deepcopy(input)
		def retVal
		try {
			retVal = getClient().put(input)
		} catch (ThrottleException e) {
			log.info("Current user throttled, moving to (${tfsUsers[currentClient]})")
			retVal = put(dInput);
		}
		return retVal
	}

	@Override
	public Object delete(Map input) {
		
		return getClient().delete(input)
	}

	@Override
	public Object patch(Map input) {
		def dInput = deepcopy(input)
		def retVal
		try {
			retVal =  getClient().patch(input);
		} catch (ThrottleException e) {
			log.info("Current user throttled, moving to (${tfsUsers[currentClient]})")
			retVal = patch(dInput);
		}
		return retVal
	}

	@Override
	public Object post(Map input) {		
		return getClient().post(input);
	}

	@Override
	public Object rateLimitPost(Map input) {
		def dInput = deepcopy(input)
		def retVal
		try {
			retVal =  getClient().rateLimitPost(input);
		} catch (ThrottleException e) {
			log.info("Current user throttled, moving to (${tfsUsers[currentClient]})")
			retVal = rateLimitPost(dInput);
		}
		return retVal
	}


	@Override
	public Object rateLimitPost(Map input, Closure encoderFunction) {
		def dInput = deepcopy(input)
		def retVal
		try {
			retVal =  getClient().rateLimitPost(input, encoderFunction);
		} catch (ThrottleException e) {
			log.info("Current user throttled, moving to (${tfsUsers[currentClient]})")
			retVal = rateLimitPost(dInput, encoderFunction);
		}
		return retVal
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
