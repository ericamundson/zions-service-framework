package com.zions.pipeline.services.cli.action.release
import org.springframework.stereotype.Component
import groovy.util.logging.Slf4j

@Component
class UobDeployableCheck extends DeployableCheck {
	def relatedFiles(File file, def files) {
		String patchFileName = "${file.path}".replace('dms', 'patches')
		File pFile = new File(patchFileName)
		if (pFile.exists()) {
			files.add(pFile)
		}

	}
}
