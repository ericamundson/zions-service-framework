package com.zions.vsts.services.permissions.db

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

import groovy.transform.Canonical

@Document
@Canonical
class FilePermissions {
	String filePath
	boolean canDelete
	List<String> authorizedUsers
	@Id
	String id
}
