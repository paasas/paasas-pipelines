package io.paasas.pipelines.util.concourse.model.task;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.paasas.pipelines.util.concourse.model.step.ContainerLimits;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TaskConfig {
	Platform platform;
	ImageResource<?> imageResource;
	List<Input> inputs;
	List<Output> outputs;
	List<Cache> caches;
	Map<String, String> params;
	Command run;
	String rootfsUri;
	ContainerLimits containerLimits;
}
