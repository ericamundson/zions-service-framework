package com.zions.ext.services.cli

import com.zions.common.services.cli.action.CliAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("clm")
@ComponentScan(["com.zions.vsts.services","com.zions.clm.services","com.zions.ext.services"])
public class CLMAppConfig {
}