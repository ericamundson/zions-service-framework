package com.zions.vsts.services.notification;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value;

import groovy.util.logging.Slf4j;
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * 
 * Service for handling notifications outside of the system, like email.
 *
 */
@Component
@Slf4j
public class NotificationService {
	@Value('${email.recipient.address:}')
	private String recipientEmailAddress
	
	@Value('${email.recipient.addresses:}')
	private String[] recipientEmailAddresses
	
	@Value('${email.sender.address:}')
	private String senderAddress

    @Autowired(required=false)
    private JavaMailSender sender

	public NotificationService() {
	}

    public String sendBuildCreatedNotification(String folder, String ciBuildDef, String releaseBuildDef, String releaseDefName) {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

		log.debug("NotificationService::sendBuildCreatedNotification -- Folder: ${folder}, CI build name: ${ciBuildDef}, Release build name: ${releaseBuildDef}")
        try {
            helper.setTo("${recipientEmailAddress}")
            String body = "The following build definitions were created from templates: \n" +
            			"Folder name: "+folder+"\n Builds: \n"
            if (ciBuildDef != "") {
            	body += "  " + ciBuildDef + "\n"
            }
            if (releaseBuildDef != "") {
            	body += "  " + releaseBuildDef + "\n"
            }
            if (releaseDefName != "") {
            	body += "\n The following release definition was created from a template: \n" +
            		"  " + releaseDefName + "\n"
            }

            body += "\n These new build and release definitions need to be reviewed for completeness and accuracy."
            helper.setText(body);
            helper.setSubject("New pipeline definitions created! Please review");

    		log.debug("NotificationService::sendBuildCreatedNotification -- Sending build created email notification")
            sender.send(message);
        //} catch (MessagingException e) {
        } catch (Exception e) {
            e.printStackTrace();
            return "error"
        }
        return "success";
    }
	
	public def sendActionCompleteNotification(String action, String phasesRun) {
		MimeMessage message = sender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		try {
            helper.setTo("${recipientEmailAddress}")
			String body = "The following batch action has completed: ${action}, phases:  ${phasesRun}."
			helper.setText(body)
			helper.setSubject("Batch ${action} completed!")
			
			sender.send(message)
		} catch (Exception e) {
			e.printStackTrace();
			return "error"
		}
		return "success";

	}

	def sendModernRequirementsFailureNotification(def msg) {
		MimeMessage message = sender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true);
		
		try {
			helper.setValidateAddresses(false)
			if (senderAddress) {
				helper.setFrom(senderAddress)
			}
			if (recipientEmailAddresses.length > 0) {
				recipientEmailAddresses.each { String address ->
					helper.addTo(address)
				}
			} else {
				helper.setTo("${recipientEmailAddress}")
			}
			String sep = System.lineSeparator()
			String body = "Zions automated monitoring has detected an outage with ModernRequirements4DevOps in ZionsETO.${sep}${sep}${msg.steps}${sep}${sep}Error:${sep}${msg.error}${sep}${sep}${msg.cause}"
			helper.setText(body)
			helper.setSubject("${msg.title} Zions Bug# ${msg.curBugId}")
			helper.addAttachment(msg.attName, msg.attFile)
			
			sender.send(message)
		} catch (Exception e) {
			e.printStackTrace();
			return "$e.message"
		}
		return "success";

	}
		
	def sendMicroServiceIssueNotification(def msg) {
		MimeMessage message = sender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		
		try {
			helper.setValidateAddresses(false)
			if (senderAddress) {
				helper.setFrom(senderAddress)
			}
			if (recipientEmailAddresses.length > 0) {
				recipientEmailAddresses.each { String address ->
					helper.addTo(address)
				}
			} else {
				helper.setTo("${recipientEmailAddress}")
			}
			Map<String, Object> headers = msg.getMessageProperties().getHeaders();
			String queue = headers['x-origin-queue']
			String error = headers['x-error']
			String trace = headers['x-trace']
			String mBody = new String(msg.body)
			def adoData = new JsonSlurper().parseText(mBody)
			String pBody = new JsonBuilder(adoData).toPrettyString()
			String sep = System.lineSeparator()
			String body = " ${error}${sep}${sep}Trace:${sep}${trace}${sep}${sep}Failed message sent to 'parked-queue'${sep}${sep}Message body:${sep}${pBody}"
			helper.setText(body)
			helper.setSubject("Micro-service on queue: ${queue}:  failed.")
			
			sender.send(message)
		} catch (Exception e) {
			e.printStackTrace();
			return "error"
		}
		return "success";

	}
}
