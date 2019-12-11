package com.zions.vsts.services.attachments

import com.zions.common.services.attachments.IAttachments
import com.zions.vsts.services.test.TestManagementService
import com.zions.vsts.services.work.FileManagementService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Sends files to be used as attachments to ADO.
 * 
 * @author z091182
 *
 */
@Slf4j
@Component
class AttachmentManagementService implements IAttachments {
	
	@Autowired
	@Value('${tfs.project:none}')
	String tfsProject
	
	@Autowired
	@Value('${tfs.collection:}')
	private String tfsCollection;

	@Autowired
	FileManagementService fileManagementService
	
	@Autowired
	TestManagementService testManagementService

	/* (non-Javadoc)
	 * @see com.zions.common.services.attachments.IAttachments#sendAttachment(java.lang.Object)
	 */
	public def sendAttachment(def info) {
		byte[] file = info.file
		if (file) {
		return fileManagementService.uploadAttachment(tfsCollection, this.tfsProject, this.tfsProject, file, file.fileName)
		} else {
			log.debug("sendAttachment was provided with a null file, skipping upload attempt")
			return null
		}
	}
	
	/* (non-Javadoc)
	 * @see com.zions.common.services.attachments.IAttachments#sendAttachment(java.lang.Object)
	 */
	public def ensureResultAttachments(def adoresult, def binaries, String rwebId) {
		if (binaries) {
			return testManagementService.ensureAttachments(adoresult, binaries, rwebId)
		} else {
			log.debug("No binaries sent")
			return null
		}
	}
	
	public def sendManualResultAttachment(adoResult, binary) {
		return testManagementService.sendManualResultAttachment(adoResult, binary)
	}

}
