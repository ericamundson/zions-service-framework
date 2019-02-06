package com.zions.vsts.services.attachments

import com.zions.common.services.attachments.IAttachments
import com.zions.vsts.services.work.FileManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Sends files to be used as attachments to ADO.
 * 
 * @author z091182
 *
 */
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

	/* (non-Javadoc)
	 * @see com.zions.common.services.attachments.IAttachments#sendAttachment(java.lang.Object)
	 */
	public def sendAttachment(def info) {
		File file = info.file
		return fileManagementService.uploadAttachment(tfsCollection, this.tfsProject, this.tfsProject, file)
	}

}
