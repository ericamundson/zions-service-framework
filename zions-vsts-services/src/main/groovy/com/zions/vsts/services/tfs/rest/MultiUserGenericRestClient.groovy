package com.zions.vsts.services.tfs.rest

import com.zions.common.services.rest.IGenericRestClient
import groovy.util.logging.Slf4j
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
		return getClient().get(input)
	}

	@Override
	public Object put(Map input) {
		
		return getClient().put(input)
	}

	@Override
	public Object delete(Map input) {
		
		return getClient().delete(input)
	}

	@Override
	public Object patch(Map input) {
		
		return getClient().patch(input)
	}

	@Override
	public Object post(Map input) {
		
		return getClient().post(input)
	}

	@Override
	public Object rateLimitPost(Map input) {
		
		return getClient().rateLimitPost(input)
	}


	@Override
	public Object rateLimitPost(Map input, Closure encoderFunction) {
		
		return getClient().rateLimitPost(input, encoderFunction);
	}

}
