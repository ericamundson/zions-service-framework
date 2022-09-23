package com.zions.common.services.user

import com.zions.common.services.ldap.User
import com.zions.common.services.ldap.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Access Zions Users via LDAP queries.
 * 
 * @author z091182
 *
 */
@Component
class UserManagementService {
	
	@Autowired(required = false)
	UserRepository userRepository
	
	User getUserById(String id) {
		return userRepository.findByUid(id)
	}
	
	User getUserByEmail(String email) {
		return userRepository.findByEmail(email)
	}

	User getUserByUidAndPassword(String uid, String password) {
		return userRepository.findByUidAndPassword(uid, password)
	}
}
