package io.paasas.pipelines;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.paasas.pipelines.cli.domain.ports.backend.Deployer;
import io.paasas.pipelines.cli.module.CommandProcessor;
import io.paasas.pipelines.cli.module.adapter.concourse.GenerateDeploymentConcoursePipeline;
import io.paasas.pipelines.cli.module.adapter.concourse.GeneratePlatformConcoursePipeline;
import io.paasas.pipelines.cli.module.adapter.google.UpdateGoogleDeployment;
import io.paasas.pipelines.cli.module.adapter.stdout.ConsoleErrorOutput;
import io.paasas.pipelines.cli.module.adapter.stdout.ConsoleOutput;
import io.paasas.pipelines.deployment.module.adapter.gcp.cloudbuild.BigQueryLiquibaseBuildTrigger;
import io.paasas.pipelines.deployment.module.adapter.gcp.cloudbuild.CloudBuildDeployer;
import io.paasas.pipelines.deployment.module.adapter.gcp.cloudrun.CloudRunDeployer;
import io.paasas.pipelines.platform.module.adapter.concourse.DeploymentConcoursePipeline;
import io.paasas.pipelines.platform.module.adapter.concourse.PlatformConcoursePipeline;

@Configuration
@EnableConfigurationProperties
public class PaasasPipelinesConfig {

	@Bean
	public ConsoleOutput output() {
		return new ConsoleOutput();
	}

	@Bean
	public ConsoleErrorOutput errorOutput() {
		return new ConsoleErrorOutput();
	}

	@Bean
	public CloudBuildDeployer cloudBuildDeployer(GcpConfiguration gcpConfiguration, ConsoleOutput consoleOutput) {
		var bigQueryLiquibaseBuildTrigger = new BigQueryLiquibaseBuildTrigger();

		return new CloudBuildDeployer(bigQueryLiquibaseBuildTrigger, gcpConfiguration, consoleOutput::println);
	}

	@Bean
	@ConfigurationProperties(prefix = "pipelines.concourse")
	public ConcourseConfiguration concourseConfiguration() {
		return new ConcourseConfiguration();
	}

	@Bean
	@ConfigurationProperties(prefix = "pipelines.gcp")
	public GcpConfiguration gcpeConfiguration() {
		return new GcpConfiguration();
	}

	@Bean
	public Deployer deployer(GcpConfiguration configuration) {
		var consoleOutput = new ConsoleOutput();

		return new CloudRunDeployer(configuration, consoleOutput::println);
	}

	@Bean
	public CommandProcessor commandProcessor(
			ConsoleErrorOutput errorOutput,
			ConsoleOutput output,
			CloudBuildDeployer cloudBuildDeployer,
			ConcourseConfiguration concourseConfiguration,
			Deployer deployer,
			GcpConfiguration gcpConfiguration) {
		var deploymentPipeline = new DeploymentConcoursePipeline(concourseConfiguration, gcpConfiguration);
		var platformPipeline = new PlatformConcoursePipeline(concourseConfiguration);

		return new CommandProcessor(
				output,
				errorOutput,
				new GenerateDeploymentConcoursePipeline(errorOutput, concourseConfiguration, deploymentPipeline),
				new GeneratePlatformConcoursePipeline(errorOutput, concourseConfiguration, platformPipeline),
				new UpdateGoogleDeployment(cloudBuildDeployer, deployer, output));
	}
}