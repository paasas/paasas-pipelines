package io.paasas.pipelines.deployment.domain.model;

import java.util.List;

import io.paasas.pipelines.deployment.domain.model.app.App;
import io.paasas.pipelines.deployment.domain.model.composer.Dags;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class DeploymentManifest {
	List<App> apps;
	List<Dags> composerDags;
	String project;
	String region;
	List<TerraformWatcher> terraform;
	
}
