package com.zions.vsts.services.notification;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value;

import groovy.util.logging.Slf4j;

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
	@Autowired
	@Value('${email.recipient.address:}')
	private String recipientEmailAddress
	
    @Autowired
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
}
