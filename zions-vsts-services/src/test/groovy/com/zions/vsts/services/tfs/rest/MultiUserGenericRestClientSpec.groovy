package com.zions.vsts.services.tfs.rest



import com.zions.common.services.rest.IGenericRestClient
import com.zions.common.services.rest.ThrottleException


import spock.lang.Specification

class MultiUserGenericRestClientSpec extends Specification {
	
	MultiUserGenericRestClient genericRestClient = new MultiUserGenericRestClient()

	def 'rateLimitPost success flow'() {
		setup: 'stub of managed generic rest clients'
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		
		and: 'Stub rest client rateLimitPost'
		int i = 1
		genericRestClient.genericRestClients.each { IGenericRestClient rc ->
			rc.rateLimitPost(_) >> {
				return [state:'complete', index: i]
			}
		}
		
		when: 'run a few call to rateLimitPost'
		def retVals = []
		for (i = 0; i < 4; i++) {
			def r = genericRestClient.rateLimitPost([url: 'http://anywhere'])
			retVals.add(r)
		}
		
		then: 'Count of runs equals 4'
		retVals.size() == 4
	}
	
	def 'rateLimitPost with throttle exception'() {
		setup: 'stub of managed generic rest clients'
		genericRestClient.tfsUsers = ['u1', 'u2', 'u3', 'u4']
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		
		and: 'Stub rest client rateLimitPost'
		genericRestClient.genericRestClients.each { IGenericRestClient rc ->
			rc.rateLimitPost(_) >> { args ->
				if (genericRestClient.tfsUsers[genericRestClient.currentClient-1] == 'u2') {
					throw new ThrottleException('Bad')
				}
				return [state:'complete', index: args[0].index]
			}
		}
		
		when: 'run a few call to rateLimitPost'
		def retVals = []
		for (int i = 0; i < 6; i++) {
			def r = genericRestClient.rateLimitPost([url: 'http://anywhere', index: i])
			retVals.add(r)
		}
		
		then: 'count of runs equals 6'
		retVals.size() == 6
	}
	
	def 'rateLimitPost success flow and encoder'() {
		setup: 'stub of managed generic rest clients'
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		
		and: 'Stub rest client rateLimitPost'
		int i = 1
		genericRestClient.genericRestClients.each { IGenericRestClient rc ->
			rc.rateLimitPost(_, _) >> {
				return [state:'complete', index: i]
			}
		}
		
		when: 'run a few call to rateLimitPost'
		def retVals = []
		for (i = 0; i < 4; i++) {
			def r = genericRestClient.rateLimitPost([url: 'http://anywhere'], null)
			retVals.add(r)
		}
		
		then: 'Count of runs equals 4'
		retVals.size() == 4
	}
	
	def 'rateLimitPost with throttle exception and encoder'() {
		setup: 'stub of managed generic rest clients'
		genericRestClient.tfsUsers = ['u1', 'u2', 'u3', 'u4']
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		
		and: 'Stub rest client rateLimitPost'
		genericRestClient.genericRestClients.each { IGenericRestClient rc ->
			rc.rateLimitPost(_, _) >> { args ->
				if (genericRestClient.tfsUsers[genericRestClient.currentClient-1] == 'u2') {
					throw new ThrottleException('Bad')
				}
				return [state:'complete', index: args[0].index]
			}
		}
		
		when: 'run a few call to rateLimitPost'
		def retVals = []
		for (int i = 0; i < 6; i++) {
			def r = genericRestClient.rateLimitPost([url: 'http://anywhere', index: i], null)
			retVals.add(r)
		}
		
		then: 'count of runs equals 6'
		retVals.size() == 6
	}
	
	def 'patch success flow'() {
		setup: 'stub of managed generic rest clients'
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		
		and: 'Stub rest client patch'
		int i = 1
		genericRestClient.genericRestClients.each { IGenericRestClient rc ->
			rc.patch(_) >> {
				return [state:'complete', index: i]
			}
		}
		
		when: 'run a few calls to patch'
		def retVals = []
		for (i = 0; i < 4; i++) {
			def r = genericRestClient.patch([url: 'http://anywhere'])
			retVals.add(r)
		}
		
		then: 'Count of runs equals 4'
		retVals.size() == 4
	}
	
	def 'patch with throttle exception'() {
		setup: 'stub of managed generic rest clients'
		genericRestClient.tfsUsers = ['u1', 'u2', 'u3', 'u4']
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		
		and: 'Stub rest client patch'
		genericRestClient.genericRestClients.each { IGenericRestClient rc ->
			rc.patch(_) >> { args ->
				if (genericRestClient.tfsUsers[genericRestClient.currentClient-1] == 'u2') {
					throw new ThrottleException('Bad')
				}
				return [state:'complete', index: args[0].index]
			}
		}
		
		when: 'run a few call to patch'
		def retVals = []
		for (int i = 0; i < 6; i++) {
			def r = genericRestClient.patch([url: 'http://anywhere', index: i])
			retVals.add(r)
		}
		
		then: 'count of runs equals 6'
		retVals.size() == 6
	}
	
	def 'put success flow'() {
		setup: 'stub of managed generic rest clients'
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		
		and: 'Stub rest client put'
		int i = 1
		genericRestClient.genericRestClients.each { IGenericRestClient rc ->
			rc.put(_) >> {
				return [state:'complete', index: i]
			}
		}
		
		when: 'run a few call to put'
		def retVals = []
		for (i = 0; i < 4; i++) {
			def r = genericRestClient.put([url: 'http://anywhere'])
			retVals.add(r)
		}
		
		then: 'Count of runs equals 4'
		retVals.size() == 4
	}

	def 'get success flow'() {
		setup: 'stub of managed generic rest clients'
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		
		and: 'Stub rest client put'
		int i = 1
		genericRestClient.genericRestClients.each { IGenericRestClient rc ->
			rc.get(_) >> {
				return [state:'complete', index: i]
			}
		}
		
		when: 'run a few call to get'
		def retVals = []
		for (i = 0; i < 4; i++) {
			def r = genericRestClient.get([url: 'http://anywhere'])
			retVals.add(r)
		}
		
		then: 'Count of runs equals 4'
		retVals.size() == 4
	}

	def 'post success flow'() {
		setup: 'stub of managed generic rest clients'
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		
		and: 'Stub rest client post'
		int i = 1
		genericRestClient.genericRestClients.each { IGenericRestClient rc ->
			rc.post(_) >> {
				return [state:'complete', index: i]
			}
		}
		
		when: 'run a few call to post'
		def retVals = []
		for (i = 0; i < 4; i++) {
			def r = genericRestClient.post([url: 'http://anywhere'])
			retVals.add(r)
		}
		
		then: 'Count of runs equals 4'
		retVals.size() == 4
	}

	def 'delete success flow'() {
		setup: 'stub of managed generic rest clients'
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		genericRestClient.genericRestClients.add(Stub(GenericRestClient))
		
		and: 'Stub rest client delete'
		int i = 1
		genericRestClient.genericRestClients.each { IGenericRestClient rc ->
			rc.delete(_) >> {
				return [state:'complete', index: i]
			}
		}
		
		when: 'run a few call to delete'
		def retVals = []
		for (i = 0; i < 4; i++) {
			def r = genericRestClient.delete([url: 'http://anywhere'])
			retVals.add(r)
		}
		
		then: 'Count of runs equals 4'
		retVals.size() == 4
	}
}
