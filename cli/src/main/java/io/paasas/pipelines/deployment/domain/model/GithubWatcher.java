package io.paasas.pipelines.deployment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class GithubWatcher {
	String branch;
	String name;
	String owner;
	String path;
	String tag;
}
