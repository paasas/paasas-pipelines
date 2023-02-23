package io.paasas.pipelines;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.paasas.pipelines.cli.domain.ports.backend.Deployer;
import io.paasas.pipelines.cli.module.CommandProcessor;
import io.paasas.pipelines.cli.module.adapter.concourse.GeneratePlatformConcoursePipeline;
import io.paasas.pipelines.cli.module.adapter.google.UpdateGoogleDeployment;
import io.paasas.pipelines.cli.module.adapter.stdout.ConsoleErrorOutput;
import io.paasas.pipelines.cli.module.adapter.stdout.ConsoleOutput;
import io.paasas.pipelines.deployment.module.CloudRunConfiguration;
import io.paasas.pipelines.deployment.module.adapter.cloudrun.CloudRunDeployer;
import io.paasas.pipelines.platform.module.ConcourseConfiguration;
import io.paasas.pipelines.platform.module.adapter.concourse.PlatformConcoursePipeline;

@Configuration
@EnableConfigurationProperties
public class PaasasPipelinesConfig {

	@Bean
	@ConfigurationProperties(prefix = "pipelines.concourse")
	public ConcourseConfiguration pipelinesConcourseConfiguration() {
		return new ConcourseConfiguration();
	}

	@Bean
	@ConfigurationProperties(prefix = "pipelines.cloudrun")
	public CloudRunConfiguration cloudRunConfiguration() {
		return new CloudRunConfiguration();
	}

	@Bean
	public Deployer deployer(CloudRunConfiguration configuration) {
		var consoleOutput = new ConsoleOutput();

		return new CloudRunDeployer(configuration, consoleOutput::println);
	}

	@Bean
	public CommandProcessor commandProcessor(
			CloudRunConfiguration cloudRunConfiguration,
			ConcourseConfiguration concourseConfiguration,
			Deployer deployer) {
		var output = new ConsoleOutput();
		var errorOutput = new ConsoleErrorOutput();

		var platformPipeline = new PlatformConcoursePipeline(concourseConfiguration);

		return new CommandProcessor(
				output,
				errorOutput,
				new GeneratePlatformConcoursePipeline(
						errorOutput,
						concourseConfiguration,
						platformPipeline),
				new UpdateGoogleDeployment(cloudRunConfiguration, deployer, output));
	}
}