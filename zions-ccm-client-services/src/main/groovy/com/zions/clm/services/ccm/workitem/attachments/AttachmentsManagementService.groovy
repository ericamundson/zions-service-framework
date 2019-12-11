package com.zions.clm.services.ccm.workitem.attachments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component;
import com.ibm.team.links.common.IReference
import com.ibm.team.repository.client.ITeamRepository
import com.ibm.team.repository.common.IContent
import com.ibm.team.workitem.client.IAuditableClient
import com.ibm.team.workitem.client.IWorkItemClient
import com.ibm.team.workitem.common.IWorkItemCommon
import com.ibm.team.workitem.common.model.IAttachment
import com.ibm.team.workitem.common.model.IAttachmentHandle
import com.ibm.team.workitem.common.model.IWorkItem
import com.ibm.team.workitem.common.model.IWorkItemReferences
import com.ibm.team.workitem.common.model.WorkItemEndPoints
import com.zions.clm.services.ccm.client.RtcRepositoryClient;
import groovy.util.logging.Slf4j
import java.io.OutputStream;

/**
 * Provides behavior to cache RTC work item attachments
 * @author z091182
 *
 */
@Component
@Slf4j
public class AttachmentsManagementService {
	
	@Autowired
	RtcRepositoryClient rtcRepositoryClient
	@Value('${cache.module:CCM}')
	String cacheModule

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
	 * @return [name: file, comment: "Added attachment ${file.name}"]
	 */
	public def cacheWorkItemAttachments(int id) {
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		IWorkItemClient workItemClient = teamRepository.getClientLibrary(IWorkItemClient.class)
		IWorkItem workItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, null);
		if (!workItem) return []
		IWorkItemCommon common = (IWorkItemCommon) teamRepository.getClientLibrary(IWorkItemCommon.class);
		IWorkItemReferences workitemReferences = common.resolveWorkItemReferences(workItem, null);
		List references = workitemReferences.getReferences(WorkItemEndPoints.ATTACHMENT);
		def files = []
		for (IReference iReference : references) {
			IAttachmentHandle attachHandle = (IAttachmentHandle) iReference.resolve();
			IAuditableClient auditableClient = (IAuditableClient) teamRepository.getClientLibrary(IAuditableClient.class);
			IAttachment attachment = (IAttachment) auditableClient.resolveAuditable((IAttachmentHandle) attachHandle,
				IAttachment.DEFAULT_PROFILE, null);
			String fName = cleanTextContent(attachment.getName())
			//def file = saveAttachment(attachment, fName, id);
			ByteArrayOutputStream s = new ByteArrayOutputStream()
			teamRepository.contentManager().retrieveContent(attachment.getContent(), s, null);
			byte[] file = s.toByteArray()
			
			if (file != null) {
				def item = [file: file, fileName: fName, comment: "Added attachment ${fName}"]
				files.add(item)
			}
		}	
		return files
	}
	
	private static String cleanTextContent(String text)
	{
		if (text.lastIndexOf('\\') > -1) {
			text = text.substring(text.lastIndexOf('\\')+1)
		}
		// strips off all non-ASCII characters
		text = text.replaceAll("[^\\x00-\\x7F]", "");
 
		// erases all the ASCII control characters
		text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
		 
		// removes non-printable characters from Unicode
		text = text.replaceAll("\\p{C}", "");
 
		return text.trim();
	}

	def saveAttachment(IAttachment attachment, String fName, int id) {
		ITeamRepository teamRepository = rtcRepositoryClient.getRepo()
		try {
			File cacheDir = new File(this.cacheLocation)
			if (!cacheDir.exists()) {
				cacheDir.mkdir();
			}
			File mDir = new File("${this.cacheLocation}${File.separator}${cacheModule}")
			if (!mDir.exists()) {
				mDir.mkdir()
			}

			File wiDir = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}")
			if (!wiDir.exists()) {
				wiDir.mkdir()
			}
			File attachmentDir = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}${File.separator}attachments")
			if (!attachmentDir.exists()) {
				attachmentDir.mkdir()
			}
			File save = new File("${this.cacheLocation}${File.separator}${cacheModule}${File.separator}${id}${File.separator}attachments${File.separator}${fName}");
			
			OutputStream out = save.newDataOutputStream()
			try {
				IContent c = attachment.getContent()
				teamRepository.contentManager().retrieveContent(attachment.getContent(), out, null);
			} finally {
				out.close();
			}
			return save
		} catch (FileNotFoundException e) {
			// do nothing
			log.warn(e.getMessage())
		} catch (IOException e) {
			// do nothing
			log.warn(e.message)
		}
	
	}

}
