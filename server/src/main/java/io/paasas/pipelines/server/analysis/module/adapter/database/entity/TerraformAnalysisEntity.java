package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import java.util.List;

import io.paasas.pipelines.server.analysis.domain.model.TerraformAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.TerraformDeployment;
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
@Entity(name = "TerraformAnalysis")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformAnalysisEntity {
	@EmbeddedId
	TerraformAnalysisKey key;

	public TerraformAnalysis to(List<TerraformDeployment> deployments) {
		return TerraformAnalysis.builder()
				.deployments(deployments)
				.packageName(key.getPackageName())
				.build();
	}

	public static TerraformAnalysisEntity from(
			PullRequestAnalysisEntity pullRequestAnalysis,
			TerraformAnalysis terraformAnalysis) {
		return TerraformAnalysisEntity.builder()
				.key(TerraformAnalysisKey.builder()
						.packageName(terraformAnalysis.getPackageName())
						.pullRequestAnalysis(pullRequestAnalysis)
						.build())
				.build();
	}
}
