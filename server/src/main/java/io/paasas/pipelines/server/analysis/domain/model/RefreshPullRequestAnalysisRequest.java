package io.paasas.pipelines.server.analysis.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder=true)
public class RefreshPullRequestAnalysisRequest {
	String commit;
	String commitAuthor;
	String manifest;
	String requester;
	String repository;
	String repositoryOwner;
	String project;
	int pullRequestNumber;
}
