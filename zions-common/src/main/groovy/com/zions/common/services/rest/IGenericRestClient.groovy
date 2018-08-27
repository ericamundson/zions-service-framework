package com.zions.common.services.rest;

import java.util.Map

public interface IGenericRestClient {

	def setProxy();
	void setCredentials(String user, String token);

	def get(Map input);

	def put(Map input);

	def delete(Map input);

	def patch(Map input);

	def post(Map input);

	def rateLimitPost(Map input);
	
}