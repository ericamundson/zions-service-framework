package com.zions.pipeline.services.yaml.template.execution
import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.zions.common.services.vault.VaultService
import com.zions.pipeline.services.mixins.FeedbackTrait
import com.zions.pipeline.services.mixins.ReadSecretsTrait
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
 *   vault:
 *     engine: secret
 *     path: zions-service-framework/dev
 *	 authorization:
 *	   scheme: UsernamePassword
 *	   parameters:
 *       #Vault key
 *	     username: ${xl.user}
 *       #Vault key      
 *		 password: ${xl.password}
 *
 * @author z091556
 *
 */
@Component
class AgentPools implements IExecutableYamlHandler, ReadSecretsTrait, FeedbackTrait {
	@Autowired
	VaultService vaultService


	@Autowired
	ProjectManagementService projectManagementService
	
	@Autowired
	EndpointManagementService endpointManagementService

	@Value('${endpoint.user:}')
	String endpointUserName

	@Value('${endpoint.password:}')
	String endpointPassword

	def handleYaml(def yaml, File containedRepo, def locations, String branch, String projectName, String pipelineId = null, String userName = null) {
		//System.out.println("In handleYaml - yaml:\n" + yaml)
		def vaultSecrets = vaultService.getSecrets(yaml.vault.engine, yaml.vault.path)
		if (yaml.authorization && yaml.authorization.parameters && yaml.authorization.parameters.username && yaml.authorization.parameters.password) {
			endpointUserName = getSecret(vaultSecrets, yaml.authorization.parameters.username)
			endpointPassword = getSecret(vaultSecrets, yaml.authorization.parameters.password)
		}
		String endpointType = yaml.endpointType
		def epAuthScheme = [ scheme: "UsernamePassword", parameters: [username: endpointUserName, password: endpointPassword] ]
		def epData = [ acceptUntrustedCerts: false ]
		def endpointData = [ administratorsGroup: null, authorization: epAuthScheme, createdBy: null, data: epData, description: yaml.endpointDescription, groupScopeId: null,	name: yaml.endpointName, operationStatus: null, readersGroup: null, serviceEndpointProjectReferences: [],type: yaml.endpointType, url: yaml.endpointUrl, isShared: false, owner: "library" ]
 
		//endpointData.publisherInputs = new JsonBuilder(yaml.publisherInputs).getContent()
		//System.out.println("In handleYaml - endpointData:\n" + endpointData)

		def epProjectReferences = []
		String projects = "${yaml.projects}"
		if ( projects == 'all') {
			def allProjects = projectManagementService.getProjects('')
			allProjects.'value'.each { project ->
				epProjectReferences = [	[ description: yaml.endpointDescription, name: yaml.endpointName, projectReference: [ id: project.id, name: project.name ] ] ]
				endpointData.serviceEndpointProjectReferences = epProjectReferences
				endpointManagementService.ensureServiceEndpoint(project.id, endpointData, yaml.grantAllPerm)
			}
		} else {
			String[] projectList = projects.split(',')
			projectList.each { String pName ->
				//System.out.println("In handleYaml - Calling projectManagementService.getProject for: " + pName)
				def project = projectManagementService.getProject('', pName)
				if (project) {
					epProjectReferences = [	[ description: yaml.endpointDescription, name: yaml.endpointName, projectReference: [ id: project.id, name: project.name ] ] ]
					endpointData.serviceEndpointProjectReferences = epProjectReferences
					endpointManagementService.ensureServiceEndpoint(pName, endpointData, yaml.grantAllPerm)
				}
			}
		}
	}
}
