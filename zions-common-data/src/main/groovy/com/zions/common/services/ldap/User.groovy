package com.zions.common.services.ldap

import groovy.transform.Canonical
import javax.naming.Name
import org.springframework.ldap.odm.annotations.Attribute
import org.springframework.ldap.odm.annotations.Entry
import org.springframework.ldap.odm.annotations.Id

@Entry(
  base = "ou=TOPS", 
  objectClasses = [ 'user' ])
@Canonical
final class User {
	@Id
	Name id
	
    @Attribute(name = "uid") String uid;
	@Attribute(name = "mail") String email;
	@Attribute(name = "displayName") String displayName;
	
}
