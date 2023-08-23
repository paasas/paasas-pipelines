package io.paasas.pipelines.server.analysis.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class TestReport {
	GitRevision gitRevision;
	String reportUrl;
	String jobUrl;
}
