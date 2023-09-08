package io.paasas.pipelines.server.analysis.domain.model;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class PullRequestAnalysis {
	List<CloudRunAnalysis> cloudRun;
	String commit;
	String commitAuthor;
	FirebaseAppAnalysis firebase;
	String manifest;
	String projectId;
	String repository;
	int pullRequestNumber;
	String requester;
	List<TerraformAnalysis> terraform;
}
