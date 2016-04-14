package com.daqsoft.schedule;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.daqsoft.schedule.server.system.RunningStatus;

@SpringBootApplication
@ComponentScan(basePackages = "com.daqsoft.schedule")
@Configuration
@EnableAutoConfiguration
public class Application extends SpringBootServletInitializer
		implements
			EmbeddedServletContainerCustomizer
{

	@Override
	protected SpringApplicationBuilder configure(
			SpringApplicationBuilder application)
	{
		return application.sources(Application.class);
	}

	public static void main(String[] args) throws Exception
	{
		ConfigurableApplicationContext web = new SpringApplicationBuilder()
				.bannerMode(Banner.Mode.OFF).sources(Application.class)
				.web(true).run(args);
		web.registerShutdownHook();
		SystemContext.context = web;
		SystemContext.waitShutdown();
	}

	@Override
	public void customize(ConfigurableEmbeddedServletContainer container)
	{
		RunningStatus.port = 8080;
		container.setPort(RunningStatus.port);
	}

}