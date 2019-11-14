package com.zions.qm.services.test.handlers

import org.springframework.stereotype.Component

@Component('QmNameHandler')
class NameHandler extends QmBaseAttributeHandler {
	static int SIZE = 255
	String badChars = 'â€œ'

	public String getQmFieldName() {
		
		return 'title'
	}

	public def formatValue(def value, def data) {
		String outVal = "${value}"
		if (outVal.length() > SIZE) {
			outVal = outVal.substring(0, SIZE-1)
		}
		//outVal = outVal.replaceAll(/[\u0022\u2018\u2019​\u201B\u201C\u201D\u201F]/, "'")
		//outVal = outVal.replaceAll(/[\u009D]/, "")
		outVal = cleanTextContent(outVal)
		return outVal;
	}
	
	private static String cleanTextContent(String text)
	{
		// strips off all non-ASCII characters
		text = text.replaceAll("[^\\x00-\\x7F]", "");
 
		// erases all the ASCII control characters
		text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
		 
		// removes non-printable characters from Unicode
		text = text.replaceAll("\\p{C}", "");
 
		return text.trim();
	}

}
