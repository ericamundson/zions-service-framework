package com.zions.clm.services.ccm.workitem.attachments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component;
import com.ibm.team.links.common.IReference
import com.ibm.team.repository.client.ITeamRepository
import com.ibm.team.workitem.client.IAuditableClient
import com.ibm.team.workitem.client.IWorkItemClient
import com.ibm.team.workitem.common.IWorkItemCommon
import com.ibm.team.workitem.common.model.IAttachment
import com.ibm.team.workitem.common.model.IAttachmentHandle
import com.ibm.team.workitem.common.model.IWorkItem
import com.ibm.team.workitem.common.model.IWorkItemReferences
import com.ibm.team.workitem.common.model.WorkItemEndPoints
import com.zions.clm.services.ccm.client.RtcRepositoryClient;
import java.io.OutputStream;

/**
 * Provides behavior to cache RTC work item attachments
 * @author z091182
 *
 */
@Component
public class AttachmentsManagementService {
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	
	String cacheLocation
	
	/**
	 * Spring constructor that auto wires the cache location.
	 * @param cacheLocation
	 */
	@Autowired
	public AttachmentsManagementService(@Value('${cache.location}') String cacheLocation) {
		this.cacheLocation = cacheLocation
	}
	
	/**
	 * Main interface method to cache RTC work item attachments.
	 * 
	 * @param id
	 * @return
	 */
	public def cacheWorkItemAttachments(int id) {
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		IWorkItem workItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, null);
		
		IWorkItemCommon common = (IWorkItemCommon) teamRepository.getClientLibrary(IWorkItemCommon.class);
		IWorkItemReferences workitemReferences = common.resolveWorkItemReferences(workItem, null);
		List references = workitemReferences.getReferences(WorkItemEndPoints.ATTACHMENT);
		for (IReference iReference : references) {
			IAttachmentHandle attachHandle = (IAttachmentHandle) iReference.resolve();
			IAuditableClient auditableClient = (IAuditableClient) teamRepository.getClientLibrary(IAuditableClient.class);
			IAttachment attachment = (IAttachment) auditableClient.resolveAuditable((IAttachmentHandle) attachHandle,
				IAttachment.DEFAULT_PROFILE, null);
			saveAttachment(attachment, id);
		}	
		
	}
	
	def saveAttachment(IAttachment attachment, int id) {
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		try {
			File cacheDir = new File(this.cacheLocation)
			if (!cacheDir.exists()) {
				cacheDir.mkdir();
			}
			
			File wiDir = new File("${this.cacheLocation}${File.separator}${id}")
			if (!wiDir.exists()) {
				wiDir.mkdir()
			}
			File attachmentDir = new File("${this.cacheLocation}${File.separator}${id}${File.separator}attachments")
			if (!attachmentDir.exists()) {
				attachmentDir.mkdir()
			}
			File save = new File("${this.cacheLocation}${File.separator}${id}${File.separator}attachments${File.separator}${attachment.getName()}");
			
			OutputStream out = save.newDataOutputStream()
			try {
				teamRepository.contentManager().retrieveContent(attachment.getContent(), out, null);
			} finally {
				out.close();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
	
	}

}
