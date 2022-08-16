package com.zions.vsts.services.permissions.db

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Component


@Component
interface FilePermissionsRepository extends MongoRepository<FilePermissions, String> {
	
	@Query("{ 'permissionId': ?0}")
	List<FilePermissions> findByPermissionId(String permissionId);

	@Query("{ 'filePath': ?0}")
	FilePermissions findByFilePath(String filePath);

}
