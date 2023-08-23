package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import java.util.List;

import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppDeployment;
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
@Entity(name = "FirebaseAppAnalysis")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FirebaseAppAnalysisEntity {
	@EmbeddedId
	FirebaseAppAnalysisKey key;

	public FirebaseAppAnalysis to(List<FirebaseAppDeployment> deployments) {
		return FirebaseAppAnalysis.builder()
				.deployments(deployments)
				.build();
	}

	public static FirebaseAppAnalysisEntity from(
			PullRequestAnalysisEntity pullRequestAnalysis,
			FirebaseAppAnalysis firebaseAppAnalysis) {
		return FirebaseAppAnalysisEntity.builder()
				.key(FirebaseAppAnalysisKey.builder()
						.pullRequestAnalysis(pullRequestAnalysis)
						.build())
				.build();
	}
}
