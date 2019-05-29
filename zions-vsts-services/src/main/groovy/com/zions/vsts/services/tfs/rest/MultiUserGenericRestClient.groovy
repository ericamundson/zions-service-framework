package com.zions.vsts.services.tfs.rest

import com.zions.common.services.rest.IGenericRestClient

class MultiUserGenericRestClient implements IGenericRestClient {
	
	List<GenericRestClient> genericClients = []
	
	int currentClient
	
	int maxClient

	@Override
	public Object setProxy() {
		// TODO Auto-generated method stub
		return null
	}

	@Override
	public void setCredentials(String user, String token) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object get(Map input) {
		// TODO Auto-generated method stub
		return null
	}

	@Override
	public Object put(Map input) {
		// TODO Auto-generated method stub
		return null
	}

	@Override
	public Object delete(Map input) {
		// TODO Auto-generated method stub
		return null
	}

	@Override
	public Object patch(Map input) {
		// TODO Auto-generated method stub
		return null
	}

	@Override
	public Object post(Map input) {
		// TODO Auto-generated method stub
		return null
	}

	@Override
	public Object rateLimitPost(Map input) {
		// TODO Auto-generated method stub
		return null
	}

}
