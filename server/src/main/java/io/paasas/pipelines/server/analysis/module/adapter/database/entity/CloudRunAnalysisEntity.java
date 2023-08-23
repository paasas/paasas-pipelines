package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import java.util.List;

import io.paasas.pipelines.server.analysis.domain.model.CloudRunAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunDeployment;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity(name = "CloudRunAnalysis")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloudRunAnalysisEntity {
	@EmbeddedId
	CloudRunAnalysisKey key;

	String tag;

	public CloudRunAnalysis to(List<CloudRunDeployment> deployments) {
		return CloudRunAnalysis.builder()
				.deployments(deployments)
				.serviceName(key.getServiceName())
				.build();
	}

	public static CloudRunAnalysisEntity from(
			PullRequestAnalysisEntity pullRequestAnalysis,
			CloudRunAnalysis cloudRunAnalysis) {
		return CloudRunAnalysisEntity.builder()
				.key(CloudRunAnalysisKey.builder()
						.pullRequestAnalysis(pullRequestAnalysis)
						.serviceName(cloudRunAnalysis.getServiceName())
						.build())
				.build();
	}
}
