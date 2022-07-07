package com.zions.vsts.services.rmq.mixins

import javax.annotation.PostConstruct

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class KafkaInitializer {
	@Value('${kafka.keytab:}')
	String keytab
	@Value('${kafka.krb5.conf:}')
	String krb5Conf

	@Value('${kafka.init.commandline:}')
	String scriptCommand
	
    @PostConstruct
    private void init() {
		System.setProperty("java.security.auth.login.config", keytab)
		System.setProperty("sun.security.krb5.debug","false")
		System.setProperty("java.security.krb5.conf", krb5Conf)

				if (scriptCommand) {
			 Process proc = scriptCommand.split(',').execute()
			 proc.waitForProcessOutput(System.out, System.err)
			 proc.waitForOrKill(100000L)
		}
    }
 
}