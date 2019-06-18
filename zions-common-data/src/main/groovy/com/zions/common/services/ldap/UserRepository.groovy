package com.zions.common.services.ldap

import org.springframework.data.ldap.repository.LdapRepository
import org.springframework.stereotype.Component

@Component
interface UserRepository extends LdapRepository<User> {
	User findByUid(String uid)
	User findByEmail(String email)
	User findByDisplayName(String displayName)
}
