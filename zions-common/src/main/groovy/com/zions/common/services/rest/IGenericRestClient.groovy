package com.zions.common.services.rest;

import java.util.Map

/**
 * Main interface to drive interaction with REST apis of various providers.
 * Providers:
 * o Atlassian Bitbucket
 * o IBM CLM
 * o Microsoft TFS/VSTS
 * @author z091182
 *
 */
public interface IGenericRestClient {

	/**
	 * Setup http client to enable network proxy interation.
	 * 
	 * @return
	 */
	def setProxy();
	
	
	/**
	 * Setup login credential for rest api.
	 * @param user - user ID
	 * @param token - Could be OAuth token or specific user password.
	 */
	void setCredentials(String user, String token);

	/**
	 * Make http GET request.
	 * @param input - object structure of request data.
	 * @return
	 */
	def get(Map input);

	/**
	 * Make http PUT request.
	 * @param input - object structure of request data.
	 * @return
	 */
	def put(Map input);
	
	/**
	 * Make http DELETE request.
	 * @param input - object structure of request data.
	 * @return
	 */
	def delete(Map input);

	/**
	 * Make http PATCH request.
	 * @param input - object structure of request data.
	 * @return
	 */
	def patch(Map input);

	/**
	 * Make http POST request.
	 * @param input - object structure of request data.
	 * @return
	 */
	def post(Map input);

	/**
	 * Make http POST request that performs checks and handles throttling.
	 * @param input - object structure of request data.
	 * @return
	 */
	def rateLimitPost(Map input);
	def rateLimitPost(Map input, Closure encoderFunction);
	
	
}