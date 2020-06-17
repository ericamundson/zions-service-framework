package com.zions.common.services.attachments

/**
 * Interface used to send an attachment to a target system for storage 
 * 
 * @author z091182
 *
 */
interface IAttachments {
	
	/**
	 * Current impl sends a File location to target.
	 * 
	 * @param info [file: <a java.io.File>]
	 * @return [url: <store location url>]
	 */
	def sendAttachment(def info)

	def sendAttachment(byte[] content, String filename)
	
	def ensureResultAttachments(def adoresult, def binaries, String rwebId)
	
	def sendManualResultAttachment(adoResult, binary)
}
