package com.zions.vsts.services.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties


@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix="policy")
public class PolicyConfigurationProperties {
	Branch branch
}
public class Branch {
	boolean enforceBuildValidation = false
	boolean enforceMinimumApprovers = false
	boolean enforceLinkedWorkItems = false
	boolean enforceMergeStrategy = false
	boolean enforceCommentResolution = false

}

class Approvers {
	
}
