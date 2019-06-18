package com.zions.common.services.ldap

import org.springframework.data.ldap.repository.LdapRepository
import org.springframework.stereotype.Component

@Component
interface UserRepository extends LdapRepository<User> {
	User findBySAMAccountName(String samAccountName)
	User findByEmail(String email)
	User findByDisplayName(String displayName)
}
