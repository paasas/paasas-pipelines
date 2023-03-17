package io.paasas.pipelines.util.concourse.model.step;

import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.paasas.pipelines.util.concourse.model.task.TaskConfig;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper=true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Task extends Step{
	String task;
	TaskConfig config;
	String file;
	String image;
	Boolean priviledged;
	Map<String, String> vars;
	ContainerLimits containerLimits;
	Map<String, String> params;
	Map<String, String> inputMapping;
	Map<String, String> outputMapping;
}
