package io.paasas.pipelines.deployment.domain.model;

import java.util.Map;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class TerraformWatcher {
	String name;
	GitWatcher git;
	String githubRepository;
	Map<String, String> vars;
}
