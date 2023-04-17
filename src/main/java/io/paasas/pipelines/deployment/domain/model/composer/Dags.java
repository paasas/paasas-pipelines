package io.paasas.pipelines.deployment.domain.model.composer;

import io.paasas.pipelines.deployment.domain.model.GitWatcher;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class Dags {
	String bucketName;
	String bucketPath;
	GitWatcher git;
	String name;
}
