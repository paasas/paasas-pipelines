package io.paasas.pipelines.util.concourse.model;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Pipeline {
	List<ResourceType> resourceTypes;
	List<Resource<?>> resources;
	List<Job> jobs;
	List<Group> groups;
}
