package io.paasas.pipelines.server.analysis.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@AllArgsConstructor
@Builder(toBuilder = true)
public class JobInfo {
	String build;
	String job;
	String pipeline;
	String projectId;
	String team;
	String url;
}
