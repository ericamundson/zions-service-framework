package com.zions.common.services.restart

/**
 * Defines interface to be used mostly by CLM interactions to handle performing restart from a
 * certain checkpoint.
 * 
 * @author z091182
 *
 */
interface IRestartManagementService {
	/**
	 * @param closure - a closure that requires two parameters, String phase, as set of query results.
	 * @return nothin
	 */
	def processPhases(Closure closure)
}
