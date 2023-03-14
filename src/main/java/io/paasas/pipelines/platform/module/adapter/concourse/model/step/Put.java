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
public final class Put extends Step {
	String put;
	String resource;
	List<Object> inputs;
	Map<String, String> params;
	Map<String, String> getParams;
	Boolean noGet;
}
