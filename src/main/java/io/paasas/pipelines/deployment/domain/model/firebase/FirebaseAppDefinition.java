package io.paasas.pipelines.deployment.domain.model.firebase;

import io.paasas.pipelines.deployment.domain.model.GitWatcher;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class FirebaseAppDefinition {
	GitWatcher git;
	String npmInstallArgs;
	String npmCommand;
}