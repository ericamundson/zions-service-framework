package com.zions.common.services.vault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import groovy.util.logging.Slf4j

@Configuration
class VaultUnsealConfig {
	@Value('${vault.unseal.elements:}')
	String[] unsealElements
	
	@Value('${vault.url:}')
	String vaultUrl

	@Bean
	VaultRestClient vaultRestClient() {
		return new VaultRestClient(vaultUrl)
	}
	
	@Bean 
	VaultService vaultService(VaultRestClient vaultRestClient) {
		VaultService service = new VaultService()
		service.vaultRestClient = vaultRestClient
		service.unsealElements = unsealElements
		//service.ensureUnsealed()
		return service
	}
	
	
}
