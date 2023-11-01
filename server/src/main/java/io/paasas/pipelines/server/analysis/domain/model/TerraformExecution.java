package io.paasas.pipelines.server.analysis.domain.model;

import java.time.LocalDateTime;
import java.util.List;

import io.paasas.pipelines.server.github.domain.model.commit.CommitState;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class TerraformExecution {
	LocalDateTime createTimestamp;
	String jobUrl;
	String packageName;
	TerraformExecutionState state;
	LocalDateTime updateTimestamp;

	public static CommitState computeCommitState(List<TerraformExecutionState> states) {
		if (states.stream()
				.filter(state -> state == TerraformExecutionState.FAILED)
				.findAny()
				.isPresent()) {
			return CommitState.FAILURE;
		}

		if (states.stream()
				.filter(state -> state == TerraformExecutionState.PENDING || state == TerraformExecutionState.RUNNING)
				.findAny()
				.isPresent()) {
			return CommitState.PENDING;
		}

		return CommitState.SUCCESS;
	}
}
