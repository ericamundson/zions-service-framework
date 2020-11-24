# Introduction

This document will detail the backend processing performed on GIT repo pipeline updates 
via processing from blueprints.

![Main Activities](Pipeline_main_activities.svg)

<div hidden>
```{r, include=FALSE}
@startuml Pipeline_main_activities.svg
start
if (doesn't have required blueprints) then (yes)
  :Create/update required blueprints;
endif
:Execute blueprints;

stop
@enduml
```
</div>

Significant processing may occur on the server due to updates to various repos from 
blueprint executions.

### Processing Pipeline Changes

![Process pipeline changes](Processing_pipeline_changes.svg)

<div hidden>
```{r, include=FALSE}
@startuml Processing_pipeline_changes.svg
actor DE as "Devops Engineer"
storage GR as "Changing GIT Repository"
storage GR2 as "Updating GIT Repository"
DE --> GR: Pull Request

package com.zions.vsts.services.rmq.mixins {
    interface MessageReceiverTrait
}

package com.zions.pipeline.services.execution.endpoint {
  component PEEP as "PipelineExecutionEndPoint"
  PEEP -[dotted]up-> MessageReceiverTrait: Implements
}
GR --> MessageReceiverTrait: ADO sends `pull request complete` event

package com.zions.pipeline.services.git {
    component GS as "GitService"
}
package com.zions.vsts.services.code {
    component CMS as "CodeManagementService"
}
PEEP -do-> CMS: Determine GIT pipeline changes for executable yaml

package com.zions.pipeline.services.yaml.execution {
    component YES as "YamlExecutionService"
}
PEEP -do-> YES: Execute any executable yaml
PEEP -do-> GS: Manage git repo changes
YES -do-> GR2: Make changes
GS -do-> GR2: Ensure ADO repo is up to date
@enduml

```
</div>

### Component: [MessageReceiverTrait](https://dev.azure.com/zionseto/DTS/_git/zions-service-framework?path=%2Fzions-vsts-microservice%2Fsrc%2Fmain%2Fgroovy%2Fcom%2Fzions%2Fvsts%2Fservices%2Frmq%2Fmixins%2FMessageReceiverTrait.groovy) and [PipelineExecutionEndPoint](https://dev.azure.com/zionseto/DTS/_git/zions-service-framework?path=%2Fzions-pipeline-execution-microservice%2Fsrc%2Fmain%2Fgroovy%2Fcom%2Fzions%2Fpipeline%2Fservices%2Fexecution%2Fendpoint%2FPipelineExecutionEndPoint.groovy)

The `MessageReceiverTrait` is critical to all communications coming from ADO via web hook events. 
 Any microservice that handles logic to process a event will implement this `mixin`.  
 This `mixin` contains the required behaviors to handle event from exchanges/queues populated 
 by event publisher handling events from ADO.  Big picture Groovy `trait` (mixin) is a way of implementing 
 multiple inheritance like `C++`.  Opinion, kills over Java.

![Web hook sequence](Webhook_sequence.svg)

<div hidden>
```{r, include=FALSE}
@startuml Webhook_sequence.svg
actor ADO as "Azure Devops"
participant EC as "EventController:eventController"
ADO -> EC: forwardADOEvent(body, request)

participant RMQE as "RabbitMQ Exchange"
EC -> RMQE: sendMessage(adoEvent)

participant RMQQ as "RabbitMQ Pull Request Completed Queue"
RMQE -> RMQQ: publish to queue (adoEvent)

participant PEEP as "<b>PipelineExecutionEndPoint</b> via <b>MessageReceiverTrait</b>"
RMQQ -> PEEP: onMessage(adoEvent)
@enduml
```
</div>

### Component: [CodeManagementService](https://dev.azure.com/zionseto/DTS/_git/zions-service-framework?path=%2Fzions-vsts-services%2Fsrc%2Fmain%2Fgroovy%2Fcom%2Fzions%2Fvsts%2Fservices%2Fcode%2FCodeManagementService.groovy)

When a pull request event is sent from ADO it contains data on commits. On pull requests 
this commit data will be used to determine changes made on repository.  If any pipeline 
executeble yaml is to be executed then it will be executed.  All executable yaml in `executables` 
folder will be executed on all pull requests.

![CodeManagementService sequence](CodeManagementService_sequence.svg)

<div hidden>
```{r, include=FALSE}
@startuml CodeManagementService_sequence.svg
participant PEEP as "PipelineExecutionEndPoint"
participant CMS as "<b>CodeManagementService</b>:codeManagmentService"
PEEP -> PEEP: getPipelineChangeLocations(): locations
activate PEEP
participant locations
PEEP -> CMS: getChanges(changesUrl) : changes
loop for (change in changes)
    PEEP -> change: getPath():path
    alt path contains pipeline path
        PEEP -> locations: add(path)
    end
end
deactivate PEEP
participant YES as "YamlExecutionService: yamlExecutionService"
PEEP -> YES: runExecutableYaml(repoUrl,name,locations, branch, project, pullRequestId)

@enduml
```
</div>

### Component: [YamlExecutionService](https://dev.azure.com/zionseto/DTS/_git/zions-service-framework?path=%2Fzions-pipeline-services%2Fsrc%2Fmain%2Fgroovy%2Fcom%2Fzions%2Fpipeline%2Fservices%2Fyaml%2Fexecution%2FYamlExecutionService.groovy)

`YamlExecutionService`'s sole purpose is to run executable yaml snippets driven by any 
changes in a GIT repo `pipeline` folder.  

![YamlExecutionService components](YamlExecutionService_components.svg)

![YamlExecutionService sequence](YamlExecutionService_sequence.svg)

<div hidden>
```{r, include=FALSE}
@startuml YamlExecutionService_components.svg
package "com.zions.pipeline.services.yaml.execution" {
  component YES as "[[https://dev.azure.com/zionseto/DTS/_git/zions-service-framework?path=%2Fzions-pipeline-services%2Fsrc%2Fmain%2Fgroovy%2Fcom%2Fzions%2Fpipeline%2Fservices%2Fyaml%2Fexecution%2FYamlExecutionService.groovy YamlExecutionService]]"
}
package "com.zions.vsts.services.admin.project" {
    component PMS as "ProjectManagementService"
}

package "com.zions.pipeline.services.git" {
    component GS as "GitService"
}
package "com.zions.pipeline.services.yaml.template.execution" {
  interface IExecutableYamlHandler
  component BP as "[[https://dev.azure.com/zionseto/DTS/_git/zions-service-framework?path=%2Fzions-pipeline-services%2Fsrc%2Fmain%2Fgroovy%2Fcom%2Fzions%2Fpipeline%2Fservices%2Fyaml%2Ftemplate%2Fexecution%2FBranchPolicy.groovy BranchPolicy]]"
  component BD as "[[https://dev.azure.com/zionseto/DTS/_git/zions-service-framework?path=%2Fzions-pipeline-services%2Fsrc%2Fmain%2Fgroovy%2Fcom%2Fzions%2Fpipeline%2Fservices%2Fyaml%2Ftemplate%2Fexecution%2FBuildDefinition.groovy BuildDefinition]]"
  component GR as "[[https://dev.azure.com/zionseto/DTS/_git/zions-service-framework?path=%2Fzions-pipeline-services%2Fsrc%2Fmain%2Fgroovy%2Fcom%2Fzions%2Fpipeline%2Fservices%2Fyaml%2Ftemplate%2Fexecution%2FGitRepository.groovy GitRepository]]"
  component RXLB as "[[https://dev.azure.com/zionseto/DTS/_git/zions-service-framework?path=%2Fzions-pipeline-services%2Fsrc%2Fmain%2Fgroovy%2Fcom%2Fzions%2Fpipeline%2Fservices%2Fyaml%2Ftemplate%2Fexecution%2FRunXLBlueprints.groovy RunXLBlueprints]]"
  component RXLDA as "[[https://dev.azure.com/zionseto/DTS/_git/zions-service-framework?path=%2Fzions-pipeline-services%2Fsrc%2Fmain%2Fgroovy%2Fcom%2Fzions%2Fpipeline%2Fservices%2Fyaml%2Ftemplate%2Fexecution%2FRunXLDeployApply.groovy RunXLDeployApply]]"
  component RXLRA as "[[https://dev.azure.com/zionseto/DTS/_git/zions-service-framework?path=%2Fzions-pipeline-services%2Fsrc%2Fmain%2Fgroovy%2Fcom%2Fzions%2Fpipeline%2Fservices%2Fyaml%2Ftemplate%2Fexecution%2FRunXLReleaseApply.groovy RunXLReleaseApply]]"
  component WI as "[[https://dev.azure.com/zionseto/DTS/_git/zions-service-framework?path=%2Fzions-pipeline-services%2Fsrc%2Fmain%2Fgroovy%2Fcom%2Fzions%2Fpipeline%2Fservices%2Fyaml%2Ftemplate%2Fexecution%2FWorkItem.groovy WorkItem]]"
  component SC as "ServiceConnection"
  component WHS as "WebHookSubscriptions"
  BP -do-> IExecutableYamlHandler
  BD --> IExecutableYamlHandler
  GR --> IExecutableYamlHandler
  RXLB -up-> IExecutableYamlHandler
  RXLDA -up-> IExecutableYamlHandler
  RXLRA -up-> IExecutableYamlHandler
  SC -up-> IExecutableYamlHandler
  WHS --> IExecutableYamlHandler
  WI -do-> IExecutableYamlHandler
  
}
YES --> IExecutableYamlHandler : Execute yaml handler on executable yaml file from GIT.
storage "GIT Repository" {
  folder "pipeline" {
      file ey as "executable.yaml"
  }
}
YES -do-> PMS: Access project data
YES -do-> GS: Load GIT repository
IExecutableYamlHandler -do-> ey : Executable yaml file in GIT repository.

@enduml

@startuml YamlExecutionService_sequence.svg
participant PEEP as "pipelineExecutionEndPoint"
participant YES as "<b>YamlExecutionService:yamlExecutionService</b>"
participant PMS as "projectManagementService"
PEEP -> YES: runExecutableYaml(repoUrl,name,locations, branch, project, pullRequestId)
YES -> PMS: getProject(name): projectData

participant CMS as "codeManagementService"
YES -> CMS: getRepo(name): repoData

participant GS as "gitService"
YES -> GS: loadChanges(repoUrl, repoName, branch)

activate YES
YES -> YES: findExecutableYaml(repo, scanLocations, projectData, repoData, pullRequestId): exeYaml
loop for (yamldata in exeYaml)
    loop for (exe in yamldata.yaml.executables)
        activate "IExecutableYamlHandler:yamlHandler"
        YES -> yamlHandlerMap: get(exe.type): yamlHandler
        alt yamlHandler != null
            YES -> "IExecutableYamlHandler:yamlHandler": handleYaml(exe, repo, scanLocations, branch, project)
        end
        deactivate "IExecutableYamlHandler:yamlHandler"
    end
end
deactivate YES
@enduml
```
</div>


## [Yaml Components](https://zionsconfluence.cs.zionsbank.com/display/SCM/DS%3A+COE%3A+Components)

Executable yaml handler documentation.
