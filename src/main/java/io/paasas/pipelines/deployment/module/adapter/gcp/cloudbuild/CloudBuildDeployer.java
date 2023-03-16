package io.paasas.pipelines.deployment.module.adapter.gcp.cloudbuild;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.devtools.cloudbuild.v1.CloudBuildClient;
import com.google.cloud.devtools.cloudbuild.v1.CloudBuildClient.ListBuildTriggersPagedResponse;
import com.google.cloud.devtools.cloudbuild.v1.CloudBuildSettings;
import com.google.cloudbuild.v1.BuildTrigger;
import com.google.cloudbuild.v1.ListBuildTriggersRequest;

import io.paasas.pipelines.GcpConfiguration;
import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.deployment.module.adapter.gcp.GcpCredentials;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudBuildDeployer {
	BigQueryLiquibaseBuildTrigger bigQueryLiquibaseBuildTrigger;
	GcpConfiguration configuration;
	Consumer<String> logger;

	public void synchronizeProjectBuilds(DeploymentManifest manifest) {
		if (manifest.getProject() == null || manifest.getProject().isBlank()) {
			throw new IllegalStateException("google project id is undefined");
		}

		try (CloudBuildClient client = client()) {
			var existingBuildTriggers = listBuildTriggers(client, manifest.getProject());

			if (manifest.getBigQuery() != null && !manifest.getBigQuery().isEmpty()) {
				synchronizeBigQueryBuildTriggers(
						existingBuildTriggers,
						manifest,
						client);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void synchronizeBigQueryBuildTriggers(
			List<BuildTrigger> existingBuildTriggers,
			DeploymentManifest manifest,
			CloudBuildClient client) {
		var existingBuildTriggersByName = existingBuildTriggers
				.stream()
				.collect(Collectors.toMap(BuildTrigger::getName, buildTrigger -> buildTrigger));

		var existingBigQueryBuildTriggers = existingBuildTriggers.stream()
				.filter(buildTrigger -> buildTrigger.getTagsList()
						.contains(BigQueryLiquibaseBuildTrigger.TAG_BIGQUERY_LIQUIBASE))
				.toList();

		var buildTriggers = manifest.getBigQuery().stream()
				.map(bigQueryLiquibaseBuildTrigger::buildTrigger)
				.toList();

		var buildTriggersToDelete = existingBuildTriggers.stream()
				.filter(existingBuildTrigger -> buildTriggers.stream()
						.noneMatch(buildTrigger -> buildTrigger.getName().equals(existingBuildTrigger.getName())))
				.toList();

		deleteBigQueryBuildTriggers(buildTriggersToDelete, client, manifest.getProject());

		var buildTriggersToUpdate = buildTriggers.stream()
				.filter(buildTrigger -> existingBigQueryBuildTriggers.stream().anyMatch(
						existingBuildTrigger -> existingBuildTrigger.getName().equals(buildTrigger.getName())))
				.map(buildTrigger -> buildTrigger.toBuilder()
						.setId(existingBuildTriggersByName.get(buildTrigger.getName()).getId())
						.build())
				.toList();

		updateBuildTriggers(buildTriggersToUpdate, client, manifest.getProject());

		var buildTriggersToCreate = buildTriggers.stream()
				.filter(buildTrigger -> existingBuildTriggers.stream()
						.noneMatch(
								existingBuildTrigger -> existingBuildTrigger.getName().equals(buildTrigger.getName())))
				.toList();

		createBuildTriggers(buildTriggersToCreate, client, manifest.getProject());
	}

	void createBuildTriggers(
			List<BuildTrigger> buildTriggers,
			CloudBuildClient client,
			String projectId) {
		buildTriggers.stream()
				.peek(buildTrigger -> logger.accept(String.format(
						"Creating cloud build trigger %s",
						buildTrigger.getName())))
				.forEach(buildTrigger -> client.createBuildTrigger(projectId, buildTrigger));
	}

	CloudBuildClient client() throws IOException {
		return CloudBuildClient.create(CloudBuildSettings.newBuilder()
				.setCredentialsProvider(GcpCredentials.credentialProviders(configuration))
				.build());
	}

	void updateBuildTriggers(List<BuildTrigger> buildTriggers, CloudBuildClient client, String projectId) {
		buildTriggers.stream()
				.peek(buildTrigger -> logger.accept(String.format(
						"Updating cloud build trigger %s",
						buildTrigger.getName())))
				.forEach(buildTrigger -> client.updateBuildTrigger(
						projectId,
						buildTrigger.getId(),
						buildTrigger));
	}

	void deleteBigQueryBuildTriggers(List<BuildTrigger> buildTriggers, CloudBuildClient client, String projectId) {
		buildTriggers.stream()
				.peek(buildTrigger -> logger.accept(String.format(
						"Deleting cloud build trigger %s (id=%s)",
						buildTrigger.getName(),
						buildTrigger.getId())))
				.forEach(buildTrigger -> client.deleteBuildTrigger(projectId, buildTrigger.getId()));
	}

	List<BuildTrigger> listBuildTriggers(CloudBuildClient client, String projectId) {
		ListBuildTriggersPagedResponse response = null;
		ArrayList<BuildTrigger> buildTriggers = new ArrayList<>();

		do {
			var requestBuilder = ListBuildTriggersRequest.newBuilder()
					.setProjectId(projectId);

			if (response != null) {
				requestBuilder.setPageToken(response.getNextPageToken());
			}

			response = client.listBuildTriggers(requestBuilder.build());

			buildTriggers.addAll(StreamSupport.stream(response.getPage().getValues().spliterator(), false)
					.filter(buildTrigger -> buildTrigger.getTagsList()
							.contains(BigQueryLiquibaseBuildTrigger.TAG_BIGQUERY_LIQUIBASE))
					.collect(Collectors.toList()));

		} while (response.getPage().hasNextPage());

		return buildTriggers;
	}
}
