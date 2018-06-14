package com.zions.clm.services.ccm.client

import com.ibm.team.repository.client.ILoginHandler2;
import com.ibm.team.repository.client.ILoginInfo2;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.login.UsernameAndPasswordLoginInfo;

public class RTCLoginHandler implements ILoginHandler2 {

	transient private String userId;
	transient private String password;
	
	public RTCLoginHandler() {
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public ILoginInfo2 challenge(ITeamRepository arg0) {
		return new UsernameAndPasswordLoginInfo(userId, password);
	}
}
