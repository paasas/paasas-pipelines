package io.paasas.pipelines.deployment.module.adapter.cloudrun;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.run.v2.Container;
import com.google.cloud.run.v2.ContainerPort;
import com.google.cloud.run.v2.CreateServiceRequest;
import com.google.cloud.run.v2.EnvVar;
import com.google.cloud.run.v2.EnvVarSource;
import com.google.cloud.run.v2.LocationName;
import com.google.cloud.run.v2.RevisionTemplate;
import com.google.cloud.run.v2.SecretKeySelector;
import com.google.cloud.run.v2.Service;
import com.google.cloud.run.v2.ServiceName;
import com.google.cloud.run.v2.ServicesClient;
import com.google.cloud.run.v2.ServicesSettings;
import com.google.cloud.run.v2.UpdateServiceRequest;
import com.google.cloud.secretmanager.v1.SecretName;

import io.paasas.pipelines.cli.domain.ports.backend.Deployer;
import io.paasas.pipelines.deployment.domain.model.App;
import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.deployment.module.CloudRunConfiguration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudRunDeployer implements Deployer {
	CloudRunConfiguration configuration;
	Consumer<String> logger;

	Container container(App app, DeploymentManifest deploymentManifest) {
		return Container.newBuilder()
				.addAllEnv(envVars(app))
				.addAllEnv(secretRefEnvVars(app, deploymentManifest))
				.setImage(app.getImage())
				.addPorts(ContainerPort.newBuilder().setName("port").setContainerPort(app.getPort()).build())
				.build();
	}

	private OperationFuture<Service, Service> createApp(
			App app,
			ServicesClient client,
			DeploymentManifest deploymentManifest) {
		var parent = LocationName.format(deploymentManifest.getProject(), deploymentManifest.getRegion());

		logger.accept("Creating new service " + parent + "/services/" + app.getName());

		return client.createServiceAsync(CreateServiceRequest.newBuilder()
				.setParent(parent)
				.setService(service(app, deploymentManifest).build())
				.setServiceId(app.getName())
				.build());
	}

	private OperationFuture<Service, Service> deleteService(
			String serviceName,
			ServicesClient client,
			DeploymentManifest deploymentManifest) {
		logger.accept("Deleting service " + serviceName);

		return client.deleteServiceAsync(serviceName);
	}

	@Override
	public void deploy(DeploymentManifest deploymentManifest) {
		try {
			var client = ServicesClient.create(ServicesSettings.newBuilder()
					.setCredentialsProvider(
							FixedCredentialsProvider
									.create(Credentials.credentials(configuration.getGoogleCredentialsJson())))
					.build());

			var listServicesResponse = client.listServices(
					LocationName.of(deploymentManifest.getProject(), deploymentManifest.getRegion()));

			var existingServicesByName = StreamSupport
					.stream(listServicesResponse.iteratePages().spliterator(), false)
					.flatMap(page -> page.getResponse().getServicesList().stream())
					.filter(service -> Optional
							.ofNullable(service.getLabelsMap().get("platform"))
							.map(label -> deploymentManifest.getProject().equals(label))
							.orElse(false))
					.collect(Collectors.toMap(Service::getName, service -> service));

			// Compute existing services
			var deleteOperations = existingServicesByName.keySet().stream()
					.filter(existingServiceName -> deploymentManifest.getApps()
							.stream()
							.noneMatch(app -> app.getName().equals(existingServiceName)))
					.map(serviceToDelete -> deleteService(serviceToDelete, client, deploymentManifest));

			// Service to create
			var createOperations = deploymentManifest.getApps().stream()
					.filter(app -> existingServicesByName.keySet().stream()
							.noneMatch(existingServiceName -> app.getName().equals(existingServiceName)))
					.map(app -> createApp(app, client, deploymentManifest));

			// Service to update
			var updateOperations = deploymentManifest.getApps().stream()
					.filter(app -> existingServicesByName.keySet().stream()
							.anyMatch(existingServiceName -> app.getName().endsWith(existingServiceName)))
					.map(app -> updateApp(app, client, deploymentManifest));

			Stream.concat(
					deleteOperations,
					Stream.concat(createOperations, updateOperations))
					.forEach(this::get);
			
			logger.accept("Deployment update succeeded");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	List<EnvVar> envVars(App app) {
		return Optional.ofNullable(app.getEnv()).orElseGet(() -> Map.of())
				.entrySet()
				.stream()
				.map(entry -> EnvVar
						.newBuilder()
						.setName(entry.getKey())
						.setValue(entry.getValue())
						.build())
				.toList();
	}

	private void get(OperationFuture<Service, Service> operation) {
		try {
			operation.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private RevisionTemplate revisionTemplate(App app, DeploymentManifest deploymentManifest) {
		return RevisionTemplate.newBuilder()
				.addContainers(Container.newBuilder()
						.setImage(app.getImage())
						.addAllEnv(envVars(app))
						.addAllEnv(secretRefEnvVars(app, deploymentManifest)))
				.build();
	}

	List<EnvVar> secretRefEnvVars(App app, DeploymentManifest deploymentManifest) {
		return Optional.ofNullable(app.getSecretEnv()).orElseGet(() -> Map.of())
				.entrySet()
				.stream()
				.map(entry -> EnvVar
						.newBuilder()
						.setName(entry.getKey())
						.setValueSource(EnvVarSource.newBuilder().setSecretKeyRef(SecretKeySelector.newBuilder()
								.setSecret(SecretName.format(deploymentManifest.getProject(), entry.getValue()))
								.build())
								.build())
						.build())
				.toList();
	}

	private Service.Builder service(App app, DeploymentManifest deploymentManifest) {
		return Service.newBuilder()
				.putLabels("platform", deploymentManifest.getProject())
				.setTemplate(revisionTemplate(app, deploymentManifest));
	}

	private OperationFuture<Service, Service> updateApp(
			App app,
			ServicesClient client,
			DeploymentManifest deploymentManifest) {
		var serviceName = ServiceName.format(
				deploymentManifest.getProject(),
				deploymentManifest.getRegion(),
				app.getName());

		logger.accept("Updating existing service " + serviceName);

		return client.updateServiceAsync(UpdateServiceRequest.newBuilder()
				.setService(service(app, deploymentManifest)
						.setName(serviceName)
						.build())
				.build());
	}
}
