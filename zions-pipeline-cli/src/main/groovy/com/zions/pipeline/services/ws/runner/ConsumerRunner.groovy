package com.zions.pipeline.services.ws.runner

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.boot.ApplicationArguments
import com.zions.common.services.cli.action.CliAction


@Component
class ConsumerRunner implements CliAction {
	
	@Value('${exe.dir:}')
	String exeDir
	
	@Value('${app.jar:}')
	String appJar
	
	@Value('${app.args:}')
	String appArgs
	
	@Value('${consumer.count:1}')
	int consumerCount
	
	def execute(ApplicationArguments args) {
		String os = System.getProperty('os.name')
		for (int i = 0; i < consumerCount; i++) {
			if (os.contains('Windows')) {
				new AntBuilder().exec(dir: "${exeDir}", executable: 'cmd', failonerror: true, spawn: true) {
					arg( line: "/c java -jar ${appJar} ${appArgs}")
				}
			} else {
				new AntBuilder().exec(dir: "${exeDir}", executable: '/bin/sh', failonerror: true, spawn: true) {
					arg( line: "-c java -jar ${appJar} ${appArgs}")
				}

			}
	
		}
		
		
		while (true) {
			System.sleep(1000000)
		}
		
	}
	
	def validate(ApplicationArguments args) {}
}
