package io.paasas.pipelines.deployment.module.adapter.gcp.cloudrun;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.run.v2.CloudSqlInstance;
import com.google.cloud.run.v2.Container;
import com.google.cloud.run.v2.ContainerPort;
import com.google.cloud.run.v2.CreateServiceRequest;
import com.google.cloud.run.v2.EnvVar;
import com.google.cloud.run.v2.EnvVarSource;
import com.google.cloud.run.v2.IngressTraffic;
import com.google.cloud.run.v2.LocationName;
import com.google.cloud.run.v2.Probe;
import com.google.cloud.run.v2.ResourceRequirements;
import com.google.cloud.run.v2.RevisionScaling;
import com.google.cloud.run.v2.RevisionTemplate;
import com.google.cloud.run.v2.SecretKeySelector;
import com.google.cloud.run.v2.SecretVolumeSource;
import com.google.cloud.run.v2.Service;
import com.google.cloud.run.v2.ServiceName;
import com.google.cloud.run.v2.ServicesClient;
import com.google.cloud.run.v2.ServicesSettings;
import com.google.cloud.run.v2.TCPSocketAction;
import com.google.cloud.run.v2.UpdateServiceRequest;
import com.google.cloud.run.v2.VersionToPath;
import com.google.cloud.run.v2.Volume;
import com.google.cloud.run.v2.VolumeMount;
import com.google.cloud.run.v2.VpcAccess;
import com.google.cloud.run.v2.VpcAccess.VpcEgress;
import com.google.cloud.secretmanager.v1.SecretName;

import io.paasas.pipelines.GcpConfiguration;
import io.paasas.pipelines.cli.domain.ports.backend.Deployer;
import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.deployment.domain.model.app.App;
import io.paasas.pipelines.deployment.domain.model.app.SecretVolume;
import io.paasas.pipelines.deployment.module.adapter.gcp.GcpCredentials;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudRunDeployer implements Deployer {
	GcpConfiguration configuration;
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
			var apps = deploymentManifest.getApps() != null ? deploymentManifest.getApps() : List.<App>of();

			var client = ServicesClient.create(ServicesSettings.newBuilder()
					.setCredentialsProvider(GcpCredentials.credentialProviders(configuration))
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
					.filter(existingServiceName -> apps
							.stream()
							.noneMatch(app -> app.getName().equals(existingServiceName)))
					.map(serviceToDelete -> deleteService(serviceToDelete, client, deploymentManifest));

			// Service to create
			var createOperations = apps.stream()
					.filter(app -> existingServicesByName.keySet().stream()
							.noneMatch(existingServiceName -> app.getName().equals(existingServiceName)))
					.map(app -> createApp(app, client, deploymentManifest));

			// Service to update
			var updateOperations = apps.stream()
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
		var image = app.getImage() + (app.getTag() != null && !app.getTag().isBlank() ? ":" + app.getTag() : "");

		var containerBuilder = Container.newBuilder()
				.setImage(image)
				.addAllEnv(envVars(app))
				.addAllEnv(secretRefEnvVars(app, deploymentManifest))
				.addAllVolumeMounts(secretVolumeMounts(app, deploymentManifest));

		if (app.getStartupProbe() != null) {
			var probeBuilder = Probe.newBuilder()
					.setFailureThreshold(app.getStartupProbe().getFailureThreshold())
					.setPeriodSeconds(app.getStartupProbe().getPeriodSeconds())
					.setTimeoutSeconds(app.getStartupProbe().getTimeoutSeconds());

			if (app.getStartupProbe().getTcpSocket() != null) {
				probeBuilder.setTcpSocket(TCPSocketAction.newBuilder()
						.setPort(app.getStartupProbe().getTcpSocket().getPort())
						.build());
			}
		}

		if (app.getResources() != null && app.getResources().getLimits() != null) {
			containerBuilder
					.setResources(ResourceRequirements.newBuilder()
							.putAllLimits(app.getResources().getLimits()));
		}

		var revisionTemplaterBuilder = RevisionTemplate.newBuilder()
				.addContainers(containerBuilder)
				.addAllVolumes(secretVolumes(app, deploymentManifest));

		if (app.getCloudSqlInstances() != null && !app.getCloudSqlInstances().isEmpty()) {
			revisionTemplaterBuilder.addVolumes(Volume.newBuilder()
					.setName("cloudsql")
					.setCloudSqlInstance(
							CloudSqlInstance.newBuilder().addAllInstances(app.getCloudSqlInstances()).build())
					.build());
		}

		if (app.getServiceAccount() != null) {
			revisionTemplaterBuilder.setServiceAccount(app.getServiceAccount());
		}

		if (app.getVpcAccessConnector() != null && !app.getVpcAccessConnector().isBlank()) {
			revisionTemplaterBuilder.setVpcAccess(VpcAccess.newBuilder()
					.setConnector(app.getVpcAccessConnector())
					.setEgress(VpcEgress.PRIVATE_RANGES_ONLY)
					.build());
		}

		if (app.getMinReplicas() != null || app.getMaxReplicas() != null) {
			var revisionScaling = RevisionScaling.newBuilder();

			if (app.getMaxReplicas() != null) {
				revisionScaling.setMaxInstanceCount(app.getMaxReplicas());
			}

			if (app.getMinReplicas() != null) {
				revisionScaling.setMinInstanceCount(app.getMinReplicas());
			}

			revisionTemplaterBuilder.setScaling(revisionScaling);
		}

		return revisionTemplaterBuilder.build();
	}

	List<VolumeMount> secretVolumeMounts(App app, DeploymentManifest deploymentManifest) {
		if (app.getSecretVolumes() == null) {
			return List.of();
		}

		return app.getSecretVolumes()
				.stream()
				.map(secretVolume -> VolumeMount.newBuilder()
						.setMountPath(secretVolume.getMountPath())
						.setName(secretVolume.getVolumeName())
						.build())
				.toList();
	}

	List<Volume> secretVolumes(App app, DeploymentManifest deploymentManifest) {
		if (app.getSecretVolumes() == null) {
			return List.of();
		}

		return IntStream.range(0, app.getSecretVolumes().size())
				.boxed()
				.map(index -> secretVolume(app.getSecretVolumes().get(index), index))
				.toList();
	}

	Volume secretVolume(SecretVolume secretVolume, int index) {
		if (secretVolume.getVolumeName() == null || secretVolume.getVolumeName().isBlank()) {
			throw new IllegalArgumentException("volume name for secret volume at index " + index + " is undefined");
		}

		if (secretVolume.getMountPath() == null || secretVolume.getMountPath().isBlank()) {
			throw new IllegalArgumentException(
					"volume mount is undefined for secret volume " + secretVolume.getVolumeName());
		}

		if (secretVolume.getSecretName() == null || secretVolume.getMountPath().isBlank()) {
			throw new IllegalArgumentException(
					"secret name is undefined for secret volume " + secretVolume.getVolumeName());
		}

		if (secretVolume.getPaths() == null || secretVolume.getPaths().isEmpty()) {
			throw new IllegalArgumentException(
					"no secret path is defined for secret volume " + secretVolume.getVolumeName());
		}

		return Volume.newBuilder()
				.setName(secretVolume.getVolumeName())
				.setSecret(SecretVolumeSource.newBuilder()
						.setSecret(secretVolume.getSecretName())
						.addAllItems(
								secretVolume.getPaths().stream()
										.map(path -> VersionToPath.newBuilder()
												.setPath(path)
												.setVersion("latest")
												.build())
										.toList()))
				.build();
	}

	List<EnvVar> secretRefEnvVars(App app, DeploymentManifest deploymentManifest) {
		if (app.getSecretEnv() == null) {
			return List.of();
		}

		return app.getSecretEnv()
				.entrySet()
				.stream()
				.map(entry -> EnvVar
						.newBuilder()
						.setName(entry.getKey())
						.setValueSource(EnvVarSource.newBuilder()
								.setSecretKeyRef(SecretKeySelector.newBuilder()
										.setSecret(SecretName.format(
												deploymentManifest.getProject(),
												entry.getValue()))
										.setVersion("latest")
										.build())
								.build())
						.build())
				.toList();
	}

	private Service.Builder service(App app, DeploymentManifest deploymentManifest) {
		var builder = Service.newBuilder()
				.putLabels("platform", deploymentManifest.getProject())
				.setIngress(IngressTraffic.INGRESS_TRAFFIC_INTERNAL_LOAD_BALANCER)
				.setTemplate(revisionTemplate(app, deploymentManifest));

		if(app.getIngressTraffic() != null) {
			builder.setIngress(app.getIngressTraffic());
		}
		
		return builder;
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
