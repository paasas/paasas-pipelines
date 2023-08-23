package io.paasas.pipelines.server.analysis.domain.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@AllArgsConstructor
@Builder(toBuilder = true)
public class DeploymentInfo {
	String build;
	String job;
	String pipeline;
	String projectId;
	String team;
	LocalDateTime timestamp;
	String url;
}
