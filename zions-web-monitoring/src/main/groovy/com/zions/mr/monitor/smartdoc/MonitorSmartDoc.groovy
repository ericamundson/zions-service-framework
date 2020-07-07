package com.zions.mr.monitor.smartdoc

import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component
import com.zions.common.services.cli.action.CliAction
import groovy.util.logging.Slf4j
import org.openqa.selenium.WebDriver
//import org.openqa.selenium.firefox.FirefoxDriver
//comment the above line and uncomment below line to use Chrome
import org.openqa.selenium.chrome.ChromeDriver

@Component
@Slf4j
class MonitorSmartDoc  implements CliAction {
	public MonitorSmartDoc() {
	}

	public def execute(ApplicationArguments data) {
		String url = data.getOptionValues('mr.url')[0]
        // declaration and instantiation of objects/variables
//    	System.setProperty("webdriver.gecko.driver","C:\\geckodriver.exe");
//		WebDriver driver = new FirefoxDriver();
		//comment the above 2 lines and uncomment below 2 lines to use Chrome
		System.setProperty("webdriver.chrome.driver","c:\\chrome-83\\chromedriver.exe");
		WebDriver driver = new ChromeDriver();
    	
        String baseUrl = 'https://login.microsoftonline.com/'
        String expectedTitle = "Welcome: Mercury Tours";
        String actualTitle = "";

        // launch Fire fox and direct it to the Base URL
        driver.get(baseUrl);

        // get the actual value of the title
        actualTitle = driver.getTitle();

        /*
         * compare the actual title of the page with the expected one and print
         * the result as "Passed" or "Failed"
         */
        if (actualTitle.contentEquals(expectedTitle)){
            System.out.println("Test Passed!");
        } else {
            System.out.println("Test Failed");
        }
       
        //close Browser
//        driver.close();
	}
	public Object validate(ApplicationArguments args) throws Exception {
		def required = ['mr.url']
		required.each { name ->
			if (!args.containsOption(name)) {
				throw new Exception("Missing required argument:  ${name}")
			}
		}
		return true
	}

}

