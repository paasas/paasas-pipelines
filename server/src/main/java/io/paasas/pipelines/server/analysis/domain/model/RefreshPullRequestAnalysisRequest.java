package io.paasas.pipelines.server.analysis.domain.model;

import io.paasas.pipelines.deployment.domain.model.deployment.JobInfo;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder=true)
public class RefreshPullRequestAnalysisRequest {
	String commit;
	String commitAuthor;
	JobInfo jobInfo;
	String manifestBase64;
	String repository;
	String project;
	int pullRequestNumber;
}
