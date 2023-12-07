package com.zions.vsts.services.policy;

public final class PolicyType {

	private PolicyType() {
		// hide constructor
	}

	// ADO policy types
	private static final String BUILD_VALIDATION_POLICY_TYPE = "0609b952-1397-4640-95ec-e00a01b2c241"
	private static final String MIN_APPROVERS_POLICY_TYPE = "fa4e907d-c16b-4a4c-9dfa-4906e5d171dd"
	private static final String LINKED_WI_POLICY_TYPE = "40e92b44-2fe1-4dd6-b3d8-74a9c21d0c6e"
	private static final String COMMENT_RES_POLICY_TYPE = "c6a1889d-b943-4856-b76f-9e46bb6b0df2"
	private static final String MERGE_STRATEGY_POLICY_TYPE = "fa4e907d-c16b-4a4c-9dfa-4916e5d171ab"
	private static final String AUTOMATICALLY_INCLUDED_REVIEWERS_POLICY_TYPE = "fd2167ab-b0be-447a-8ec8-39368250530e"
	private static final String CUSTOM_STATUS_POLICY_TYPE = "cbdc66da-9728-4af8-aada-9a5a32e4a226"

	private static final String CHECKMARX_STATUS_NAME = "checkmarx"
	private static final String SAST_STATUS_GENRE = "sast"
	private static final String SNOWCI_STATUS_NAME = "snowci-check"
	private static final String WATCHDOG_STATUS_NAME = "unauthorized-changes"
	private static final String CI_STATUS_GENRE = "continuous-integration"
}