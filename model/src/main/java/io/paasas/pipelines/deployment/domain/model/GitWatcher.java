package io.paasas.pipelines.deployment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class GitWatcher {
	String branch;
	String cron;
	String name;
	String path;
	String uri;
	String tag;
}
