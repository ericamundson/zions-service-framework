package com.zions.common.services.vault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.zions.common.services.rest.AGenericRestClient
import com.zions.common.services.rest.ARESTClient

@Component
class VaultRestClient extends AGenericRestClient {
	String vaultUrl
	
	public VaultRestClient(@Value('${vault.url:}') String url) {
		if (!url || url.length() == 0) {
			url = 'http://utmvpi0144:8200'
		}
		this.vaultUrl = url
		delegate = new ARESTClient(vaultUrl)
		delegate.ignoreSSLIssues()
		delegate.handler.failure = { it }
		setProxy()
	}
	
	public void setCredentials(String user, String token) {}
	
}
