package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import io.paasas.pipelines.server.analysis.domain.model.TerraformPlanExecution;
import jakarta.persistence.Embedded;
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
@Entity(name = "TerraformPlanExecution")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformPlanExecutionEntity {
	@EmbeddedId
	TerraformExecutionKey key;

	@Embedded
	TerraformExecutionEntity execution;

	String commitId;

	public TerraformPlanExecution to() {
		return TerraformPlanExecution.builder()
				.execution(execution.to(key.getPackageName()))
				.commitId(commitId)
				.build();
	}

	public static TerraformPlanExecutionEntity create(
			String packageName,
			PullRequestAnalysisEntity pullRequestAnalysis) {
		return TerraformPlanExecutionEntity.builder()
				.commitId(pullRequestAnalysis.getCommit())
				.execution(TerraformExecutionEntity.create())
				.key(TerraformExecutionKey.builder()
						.packageName(packageName)
						.pullRequestAnalysis(pullRequestAnalysis)
						.build())
				.build();
	}
}
