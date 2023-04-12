package io.paasas.pipelines.deployment.domain.model;

import java.util.List;

import io.paasas.pipelines.deployment.domain.model.app.App;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class DeploymentManifest {
	List<App> apps;
	String project;
	String region;
	List<TerraformWatcher> terraform;
}
