package io.paasas.pipelines.deployment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class TestGitWatcher {
	String branch;
	String cron;
	String extraMavenOpts;
	String name;
	String path;
	String reportDomain;
	String uri;
	String tag;
}
