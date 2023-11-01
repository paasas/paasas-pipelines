package io.paasas.pipelines.server.analysis.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.ToString.Exclude;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class PullRequestAnalysis {
	List<CloudRunAnalysis> cloudRun;
	Integer commentId;
	String commit;
	String commitAuthor;
	FirebaseAppAnalysis firebase;
	PullRequestAnalysisJobInfo jobInfo;
	
	@Exclude
	String manifest;
	
	String projectId;
	String repository;
	int pullRequestNumber;
	String requester;
	List<TerraformAnalysis> terraform;
}
