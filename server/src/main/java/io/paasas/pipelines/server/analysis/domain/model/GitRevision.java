package io.paasas.pipelines.server.analysis.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class GitRevision {
	String branch;
	String commit;
	String commitAuthor;
	String path;
	String repositoryOwner;
	String repository;
	String tag;
}
