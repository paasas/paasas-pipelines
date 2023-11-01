package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import java.time.LocalDateTime;

import io.paasas.pipelines.server.analysis.domain.model.TerraformExecution;
import io.paasas.pipelines.server.analysis.domain.model.TerraformExecutionState;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformExecutionEntity {
	LocalDateTime createTimestamp;

	@Enumerated(EnumType.STRING)
	TerraformExecutionState state;

	@Column(length = 65535)
	String jobUrl;

	LocalDateTime updateTimestamp;

	public TerraformExecution to(String packageName) {
		return TerraformExecution.builder()
				.createTimestamp(createTimestamp)
				.packageName(packageName)
				.state(state)
				.updateTimestamp(updateTimestamp)
				.build();
	}

	public static TerraformExecutionEntity create() {
		var createTimestamp = LocalDateTime.now();

		return TerraformExecutionEntity.builder()
				.createTimestamp(createTimestamp)
				.state(TerraformExecutionState.PENDING)
				.updateTimestamp(createTimestamp)
				.build();
	}
}
