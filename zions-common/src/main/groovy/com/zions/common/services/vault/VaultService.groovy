package com.zions.common.services.vault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import org.springframework.core.env.Environment

@Component
class VaultService {
	
	@Autowired
	Environment env
	
	@Autowired
	VaultRestClient vaultRestClient
	@Value('${vault.unseal.elements:}')
	String[] unsealElements
	
	boolean isSealed() {
		boolean iSealed = false
		def result = vaultRestClient.get(
			uri: "${vaultRestClient.vaultUrl}/v1/sys/seal-status",
			contentType: ContentType.JSON
			)
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
	
	def getSecrets(String engine, String path) {
		String vtoken = env.getProperty('spring.cloud.vault.token')
		String[] parts = path.split('/')
		String opath = ""
		Map out = [:]
		for (String part in parts) {
			opath += part
			def result = vaultRestClient.get(
				//contentType: ContentType.JSON,
				uri: "${vaultRestClient.vaultUrl}/v1/${engine}/data/${opath}",
				headers: ['X-Vault-Token': vtoken, Accept: 'application/json'],
				)
			if (result.data && result.data.data) {
				def amap = result.data.data
				amap.each { key, val ->
					out[key] = val
				}
			}
			opath += '/'
		}
		return out
	}
	
	void ensureUnsealed() {
		if (isSealed()) {
			unseal()
		}
	}
}
