package com.zions.common.services.vault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.zions.common.services.rest.AGenericRestClient
import com.zions.common.services.rest.ARESTClient

import groovy.util.logging.Slf4j

@Component
@Slf4j
class VaultRestClient extends AGenericRestClient {
	String vaultUrl
	
	@Value('${vault.use.proxy:true}')
	boolean useProxy
	
	public VaultRestClient(@Value('${vault.url:}') String url) {
		if (!url || url.length() == 0) {
			url = 'http://utmvpi0144:8200'
		}
		this.vaultUrl = url
		delegate = new ARESTClient(vaultUrl)
		try {
			delegate.ignoreSSLIssues()
		} catch (e) {}
		delegate.handler.failure = { resp ->
			if (resp.entity) {
				def outputStream = new ByteArrayOutputStream()
				resp.entity.writeTo(outputStream)
				def errorMsg = outputStream.toString('utf8')
				if (resp.status == 400 || resp.status == 412)
					log.info("ADO Http ${resp.status} response:  ${errorMsg}")
				else
					log.error("ADO Http ${resp.status} error response:  ${errorMsg}")
			}
			return resp
		}
		if (useProxy) {
			setProxy()
		}
	}
	
	public void setCredentials(String user, String token) {}
	
}
