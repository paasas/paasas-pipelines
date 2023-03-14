package io.paasas.pipelines.platform.module.adapter.concourse.model.step;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper=true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class SetPipeline extends Step {
	String setPipeline;
	String file;
	Map<String, String> instanceVars;
	Map<String, String> vars;
	List<String> varFiles;
	String team;
}
