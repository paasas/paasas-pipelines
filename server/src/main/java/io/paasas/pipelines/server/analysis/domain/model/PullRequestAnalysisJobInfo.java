package io.paasas.pipelines.server.analysis.domain.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class PullRequestAnalysisJobInfo {
	String build;
	String job;
	String pipeline;
	String team;
	LocalDateTime timestamp;
	String url;
}
