package com.zions.jama.services.requirements.handlers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.zions.jama.services.requirements.JamaRequirementsManagementService

@Component
class CreatorHandler extends RmBaseAttributeHandler {
      @Autowired
      JamaRequirementsManagementService jamaRequirementsManagementService
      
      @Override
      public String getFieldName() {
            
            return 'createdBy'
      }

      @Override
      public Object formatValue(Object value, Object itemData) {
           	String email = jamaRequirementsManagementService.getUserEmail(value)
            String outVal
			if (email) {
				outVal = email.toLowerCase()
			}
            return outVal;
      }

}
