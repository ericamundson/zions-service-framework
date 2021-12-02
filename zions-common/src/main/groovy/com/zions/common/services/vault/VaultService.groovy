package com.zions.common.services.vault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import org.springframework.core.env.Environment

@Component
@Slf4j
class VaultService {
	
	@Autowired
	Environment env
	
	@Autowired
	VaultRestClient vaultRestClient
	@Value('${vault.unseal.elements:}')
	String[] unsealElements
	
	@Value('${spring.cloud.vault.token:}')
	String vaultToken
	
	boolean isSealed() {
		boolean iSealed = false
		def result = vaultRestClient.get(
			uri: "${vaultRestClient.vaultUrl}/v1/sys/seal-status",
			contentType: ContentType.JSON
			)
		if (!result) return true
		if (result && result.sealed) return true
		return false
	}
	
	void unseal() {
		boolean iSealed = false
		unsealElements.each { key -> 
			def result = vaultRestClient.put(
				uri: "${vaultRestClient.vaultUrl}/v1/sys/unseal",
				contentType: ContentType.JSON,
				requestContentType: ContentType.JSON,
				body: [ key: key ]
				)
		}
	}
	
	def getSecrets(String engine, String path, boolean rollup = true, boolean actual = false) {
		String vtoken = env.getProperty('spring.cloud.vault.token')
		if (rollup) {
			String[] parts = path.split('/')
			String opath = ""
			Map out = [:]
			for (String part in parts) {
				opath += part
				def result = vaultRestClient.get(
					//contentType: ContentType.JSON,
					uri: "${vaultRestClient.vaultUrl}/v1/${engine}/data/${opath}",
					headers: ['X-Vault-Token': vaultToken, Accept: 'application/json'],
					)
				if (result && result.data && result.data.data) {
					def amap = result.data.data
					amap.each { key, val ->
						out[key] = val
					}
				}
				opath += '/'
			}
			if (out.size() == 0) return null
			return out
		} else {
			def result = vaultRestClient.get(
				//contentType: ContentType.JSON,
				uri: "${vaultRestClient.vaultUrl}/v1/${engine}/data/${path}",
				headers: ['X-Vault-Token': vaultToken, Accept: 'application/json'],
				)
				
			if (actual) return result
			if (result && result.data && result.data.data) {
				return result.data.data
			}

		}
		return null
	}
	
	def getSecrets(String engine, String[] paths) {
		Map out = [:]
		for (String path in paths) {
			def amap = getSecrets(engine, path)
			if (amap) {
				amap.each { key, val ->
					out[key] = val
				}
			}
		}
		return out
	}
	
	def ensureSecrets(String engine, String path, Map data) {
		String vtoken = env.getProperty('spring.cloud.vault.token')
		Map cData = getSecrets(engine, path, false, true)
		Map oData = data
		if (cData && cData.data && cData.data.data) {
			oData = cData.data.data
			data.each { key, val ->
				oData[key] = val
			}
		} 
		def body = [options: [cas: 0], data: oData]
		def result = null
		if (!cData) {
			result = vaultRestClient.post(
					requestContentType: ContentType.JSON,
					uri: "${vaultRestClient.vaultUrl}/v1/${engine}/data/${path}",
					headers: ['X-Vault-Token': vaultToken, Accept: 'application/json'],
					body: body
					)
		} else {
			int version = cData.data.metadata.version
			body.options.cas = version
			result = vaultRestClient.put(
				requestContentType: ContentType.JSON,
				uri: "${vaultRestClient.vaultUrl}/v1/${engine}/data/${path}",
				headers: ['X-Vault-Token': vaultToken, Accept: 'application/json'],
				body: body
				)

		}
		return result
	}
	
	void ensureUnsealed() {
		while (true) {
			try {
				if (isSealed()) {
					unseal()
				}
				System.sleep(60000)
			} catch (e) {
				log.error(e.message)
			}
		}
	}
}
