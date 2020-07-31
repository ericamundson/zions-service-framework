package com.zions.common.services.vault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType

@Component
class VaultService {
	
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
	
	void ensureUnsealed() {
		if (isSealed()) {
			unseal()
		}
	}
}
