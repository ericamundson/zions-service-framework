package com.zions.pipeline.services.mixins
import org.springframework.beans.factory.annotation.Autowired

import com.zions.common.services.vault.VaultService

trait ReadSecretsTrait {
	@Autowired
	VaultService vaultService

	String getSecret(String engine, String path, String value) {
		def vaultSecrets = vaultService.getSecrets(engine, path)
		if (value.startsWith('${') && vaultSecrets) {
			String name = value.substring('${'.length())
			name = name.substring(0, name.length() - 1)
			return vaultSecrets[name]
		}
		null
	}
	
	String getSecret(def vaultSecrets, String value) {
		if (value.startsWith('${') && vaultSecrets) {
			String name = value.substring('${'.length())
			name = name.substring(0, name.length() - 1)
			return vaultSecrets[name]
		}
		null
	}
}
