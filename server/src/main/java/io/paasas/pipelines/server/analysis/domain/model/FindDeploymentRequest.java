package io.paasas.pipelines.server.analysis.domain.model;

public record FindDeploymentRequest(
		String gitUri,
		String gitPath,
		String gitTag) {

}
