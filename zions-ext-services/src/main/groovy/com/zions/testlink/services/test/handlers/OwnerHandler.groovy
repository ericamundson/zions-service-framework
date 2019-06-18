package com.zions.testlink.services.test.handlers

import com.zions.common.services.ldap.User
import com.zions.common.services.user.UserManagementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component('TlOwnerHandler')
class OwnerHandler extends TlBaseAttributeHandler {
	@Autowired
	UserManagementService userManagementService
	
	public String getFieldName() {
		
		return 'owner'
	}

	public def formatValue(def value, def data) {
		def itemData = data.itemData
		String authorLogin = "${itemData.authorLogin}"
		if (authorLogin == null || authorLogin.length() == 0) return null
		User user = userManagementService.getUserById(authorLogin)
		if (!user) return null
		return user.email;
	}

}
