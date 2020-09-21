package com.zions.pipeline.services.yaml.template.execution
import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component


import com.zions.vsts.services.admin.project.ProjectManagementService
import com.zions.vsts.services.endpoint.EndpointManagementService

/**
 * Accepts yaml in the form (some items will vary by endpointType and authorization scheme):
 * executables:
 * - type: serviceConnection
 *   context: ZionsETO
 *   projects: ReleaseEngineering
 *   endpointType: xldserver
 *   endpointUrl: https://xldeploy.cs.zionsbank.com
 *   endpointName: XL Deploy - Prod
 *   endpointDescription: XL Deploy for production environment
 *   grantAllPerm: true
 *	 authorization:
 *	   scheme: UsernamePassword
 *	   parameters:
 *	     username: svc-xld-user
 *		 password: password
 *
 * @author z091556
 *
 */
@Component
class ServiceConnection implements IExecutableYamlHandler {
	

	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	EndpointManagementService endpointManagementService

	@Autowired
	@Value('${webhook.user:}')
	String endpointUserName

	@Autowired
	@Value('${webhook.password:}')
	String endpointPassword

	def handleYaml(def yaml, File containedRepo, def locations, String branch) {
		//System.out.println("In handleYaml - yaml:\n" + yaml)
		// TODO: Collection of event types dosn't makes sense as each different event type will have a different set of publisherInputs
		String endpointType = yaml.eventType
		def epAuthScheme = [ scheme: "UsernamePassword", parameters: [username: endpointUserName, password: endpointPassword] ]
		def epData = [ acceptUntrustedCerts: false ]
		def endpointData = [ administratorsGroup: null, authorization: epAuthScheme, createdBy: null, data: epData, description: yaml.endpointDescription, groupScopeId: null,	name: yaml.endpointName, operationStatus: null, readersGroup: null, serviceEndpointProjectReferences: [],type: yaml.endpointType, url: yaml.endpointUrl, isShared: false, owner: "library" ]
 
		endpointData.publisherInputs = new JsonBuilder(yaml.publisherInputs).getContent()
		//System.out.println("In handleYaml - endpointData:\n" + endpointData)

		def epProjectReferences = []
		String projects = "${yaml.projects}"
		if ( projects == 'all') {
			def allProjects = projectManagementService.getProjects('')
			allProjects.'value'.each { project ->
				epProjectReferences = [	[ description: yaml.endpointDescription, name: yaml.endpointDescription, projectReference: [ id: project.id, name: project.name ] ] ]
				endpointData.serviceEndpointProjectReferences = epProjectReferences
				endpointManagementService.ensureServiceEndpoint(project, endpointData, yaml.grantAllPerm)
			}
		} else {
			String[] projectList = projects.split(',')
			projectList.each { String pName ->
				//System.out.println("In handleYaml - Calling projectManagementService.getProject for: " + pName)
				def project = projectManagementService.getProject('', pName)
				if (project) {
					epProjectReferences = [	[ description: yaml.endpointDescription, name: yaml.endpointDescription, projectReference: [ id: project.id, name: project.name ] ] ]
					endpointData.serviceEndpointProjectReferences = epProjectReferences
					endpointManagementService.ensureServiceEndpoint(project, endpointData, yaml.grantAllPerm)
				}
			}
		}
	}
}
