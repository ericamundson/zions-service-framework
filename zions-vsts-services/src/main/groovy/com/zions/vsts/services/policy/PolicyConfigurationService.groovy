package com.zions.vsts.services.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PolicyConfigurationService {
	
	@Autowired
	PolicyManagementConfiguredService policyManagementConfiguredService
	
	@Autowired
	PolicyConfigurationProperties policyConfigurationProperties
	
}
