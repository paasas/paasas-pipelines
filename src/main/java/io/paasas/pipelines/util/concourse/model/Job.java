package io.paasas.pipelines.util.concourse.model;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.paasas.pipelines.util.concourse.model.step.Step;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Job {
	String name;
	List<Step> plan;
	Step onSuccess;
	Step onFailure;
}
