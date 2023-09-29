package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import io.paasas.pipelines.server.analysis.domain.model.TerraformExecution;
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
@Entity(name = "TerraformApplyExecution")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformApplyExecutionEntity {
	@EmbeddedId
	TerraformExecutionKey key;

	@Embedded
	TerraformExecutionEntity execution;

	public TerraformExecution to() {
		return execution.to(key.getPackageName());
	}

	public static TerraformApplyExecutionEntity create(
			String packageName,
			PullRequestAnalysisEntity pullRequestAnalysis) {
		return TerraformApplyExecutionEntity.builder()
				.execution(TerraformExecutionEntity.create())
				.key(TerraformExecutionKey.builder()
						.packageName(packageName)
						.pullRequestAnalysis(pullRequestAnalysis)
						.build())
				.build();
	}
}
