package com.zions.vsts.services.test

import com.zions.common.services.test.Generator
import org.springframework.stereotype.Component

@Component
class WiTypeGenerator implements Generator {
	List<String> types = ['Test Case', 'Test Plan']
	Random r = new Random()
	@Override
	public Object gen() {
		int i = types.size()
		int index = r.nextInt(i)
		return types.get(index)
	}

}
