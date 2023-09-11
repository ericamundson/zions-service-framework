package com.zions.vsts.services.health

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import com.zions.common.services.vault.VaultService;
import com.zions.common.services.vault.VaultRestClient;
import javax.annotation.PostConstruct
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import groovyx.net.http.AsyncHTTPBuilder
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import java.util.concurrent.FutureTask

@Component
@ConditionalOnEnabledHealthIndicator('vaultDetails')
@Slf4j
class VaultDetailsIndicator implements HealthIndicator {
	
	@Value('${vault.url:}')
	String vaultUrl
	
	@Value('${spring.cloud.vault.token:}')
	String vaultToken
	
	@Value('${tfs.token:#{null}}')
	String tfsToken

	InternalVaultRestClient vaultClient
	
	@PostConstruct
	def init() {
		vaultClient = new InternalVaultRestClient(vaultUrl, vaultToken)

	}
	
	Health health() {
		boolean sealed = vaultClient.isSealed()
		Health.Builder status = Health.up()
		if (sealed) {
			status = Health.down(new Exception('Vault is sealed'))
		}
		Map items = [:]
		try {
			items = vaultClient.getPath('zions-service-framework')
		} catch (e) {
			//status.down(new Exception('zions-service-framework path not found!'))			
		}
		if (items.empty) {
			status.down(new Exception('zions-service-framework path not found!'))
		}
		if (tfsToken == null) {
			status.down(new Exception('No ADO Token!'))			
		}
		Map<String, Object> details = new HashMap<>();
		details.put("sealed", "${sealed}");
		details.put("strategy", "thread-local");

		return status.withDetails(details).build();
	}
}

@Slf4j
class InternalVaultRestClient {
	AsyncHTTPBuilder delegate 
	String vaultUrl
	String vaultToken
	public InternalVaultRestClient(String url, String vaultToken) {
		if (!url || url.length() == 0) {
			url = 'http://utmvpi0144:8200'
		}
		this.vaultUrl = url
		this.vaultToken = vaultToken
		delegate = new AsyncHTTPBuilder(uri: url)
		log.info("Vault URL:  ${vaultUrl}")
		try {
			if (vaultUrl.startsWith('https')) {
				delegate.ignoreSSLIssues()
			}
		} catch (e) { }
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
	}

	boolean isSealed() {
	
		boolean iSealed = false
		FutureTask task = delegate.get(
			uri: "${vaultUrl}/v1/sys/seal-status",
			contentType: ContentType.JSON
			)
		def result = task.get()
		if (!result) return true
		if (result && result.sealed) return true
		return false

	}
	
	Map getPath(String path) {
		FutureTask task = delegate.get(
			//contentType: ContentType.JSON,
			uri: "${vaultUrl}/v1/secret/data/${path}",
			headers: ['X-Vault-Token': vaultToken, Accept: 'application/json'],
			)
		Map result = task.get()
		if (!result) return []
		return result
	}
}
