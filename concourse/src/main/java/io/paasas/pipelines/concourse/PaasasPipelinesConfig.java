package io.paasas.pipelines.concourse;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.paasas.pipelines.concourse.command.CommandProcessor;
import io.paasas.pipelines.concourse.command.GeneratePlatformPipeline;
import io.paasas.pipelines.concourse.output.ConsoleErrorOutput;
import io.paasas.pipelines.concourse.output.ConsoleOutput;

@Configuration
@EnableConfigurationProperties
public class PaasasPipelinesConfig {

	@Bean
	@ConfigurationProperties(prefix = "pipelines")
	public PipelinesConcourseConfiguration pipelinesConcourseConfiguration() {
		return new PipelinesConcourseConfiguration();
	}

	@Bean
	public CommandProcessor commandProcessor(PipelinesConcourseConfiguration pipelinesConcourseConfiguration) {
		var output = new ConsoleOutput();
		var errorOutput = new ConsoleErrorOutput();

		var platformPipeline = new PlatformPipeline(pipelinesConcourseConfiguration);

		return new CommandProcessor(output, errorOutput, new GeneratePlatformPipeline(errorOutput, platformPipeline));
	}
}
